package rs.skola.platforma.rotacija;

import org.springframework.stereotype.Component;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.raspored.domain.Dan;
import rs.skola.platforma.rotacija.domain.RotDodela;
import rs.skola.platforma.rotacija.domain.RotPredmet;
import rs.skola.platforma.rotacija.domain.Rotacija;
import rs.skola.platforma.rotacija.web.DetekcijaVezbiResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Blok-bazirani algoritam dodele grupa na termine vezbi.
 *
 * Princip:
 *   1. Za svakog ukljucenog profesora, sortiramo njegove termine vezbi (dan, cas)
 *      i delimo ih po predmetima (prvih {@code casovaNedeljno} prvog predmeta,
 *      sledecih toliko drugog, ...).
 *   2. Termini se grupisu u "blokove" — uzastopni casovi istog profesora,
 *      istog predmeta i istog dana ide istoj grupi. To prati realnost vezbi
 *      koje su tipicno 2-3 casa zaredom u istom danu.
 *   3. Kroz N nedelja, blokove rotiramo linearno: grupa = ((i + w × broj_blokova) mod G) + 1.
 *      Time se postize:
 *      - u jednoj nedelji ista grupa se ne ponavlja kod razlicitih blokova
 *      - kroz ciklus, svaka grupa dobija priblizno isti broj blokova
 *
 * Primeri (G grupa, N nedelja, C casova nedeljno, B blokova nedeljno):
 *   - C=9, G=3, N=1, B=3 (3 dana × 3 casa): svaka nedelja G1, G2, G3
 *   - C=6, G=3, N=3, B=2 (2 dana × 3 casa): "G1G2, G3G1, G2G3"
 *   - C=3, G=3, N=3, B=1 (1 dan × 3 casa):  "G1, G2, G3"
 */
@Component
public class RotacijaAlgoritam {

    public List<RotDodela> generisi(Rotacija r, DetekcijaVezbiResponse detekcija) {
        int g = r.getBrojGrupa();
        int n = r.getBrojNedelja();

        // 1. Mapiranje profesor -> Korisnik (samo za ukljucene)
        Map<UUID, Korisnik> profesoriMap = new HashMap<>();
        for (RotPredmet p : r.getPredmeti()) {
            profesoriMap.put(p.getProfesor().getId(), p.getProfesor());
        }

        // 2. Predmeti po profesoru, sortirani po redniBroj
        Map<UUID, List<RotPredmet>> predmetiPoProfesoru = new HashMap<>();
        for (RotPredmet p : r.getPredmeti()) {
            predmetiPoProfesoru
                    .computeIfAbsent(p.getProfesor().getId(), k -> new ArrayList<>())
                    .add(p);
        }
        predmetiPoProfesoru.values().forEach(lista -> lista.sort(Comparator.comparing(RotPredmet::getRedniBroj)));

        // 3. Termini po profesoru — samo ukljuceni profesori
        Map<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> terminePoProfesoru = new HashMap<>();
        for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
            for (UUID profId : t.profesoriIds()) {
                if (profId == null || !profesoriMap.containsKey(profId)) continue;
                terminePoProfesoru.computeIfAbsent(profId, k -> new ArrayList<>()).add(t);
            }
        }
        terminePoProfesoru.values().forEach(lista ->
                lista.sort(Comparator
                        .comparing((DetekcijaVezbiResponse.TerminVezbi t) -> t.dan().ordinal())
                        .thenComparing(DetekcijaVezbiResponse.TerminVezbi::cas)));

        // 4. Po profesoru, dodeli svakom terminu njegov predmet (sekvencijalno).
        // Mapiraj (profesorId, dan, cas) -> RotPredmet.
        Map<KljucTerminProfesor, RotPredmet> terminPredmet = new HashMap<>();
        for (Map.Entry<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoProfesoru.entrySet()) {
            UUID profId = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            List<RotPredmet> predmeti = predmetiPoProfesoru.getOrDefault(profId, List.of());
            int idx = 0;
            for (RotPredmet pred : predmeti) {
                for (int i = 0; i < pred.getCasovaNedeljno() && idx < termini.size(); i++, idx++) {
                    DetekcijaVezbiResponse.TerminVezbi t = termini.get(idx);
                    terminPredmet.put(new KljucTerminProfesor(t.dan(), t.cas(), profId), pred);
                }
            }
        }

        // 5. Grupisi u blokove: (profesor, predmet, dan) -> lista termina (sortiranih po cas).
        // LinkedHashMap zadrzava insertion order — stabilan redosled za rotaciju.
        Map<BlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> blokovi = new LinkedHashMap<>();
        List<DetekcijaVezbiResponse.TerminVezbi> sortiraniTermini = new ArrayList<>(detekcija.termini());
        sortiraniTermini.sort(Comparator
                .comparing((DetekcijaVezbiResponse.TerminVezbi t) -> t.dan().ordinal())
                .thenComparing(DetekcijaVezbiResponse.TerminVezbi::cas));

        for (DetekcijaVezbiResponse.TerminVezbi t : sortiraniTermini) {
            for (UUID profId : t.profesoriIds()) {
                if (profId == null || !profesoriMap.containsKey(profId)) continue;
                RotPredmet pred = terminPredmet.get(new KljucTerminProfesor(t.dan(), t.cas(), profId));
                if (pred == null) continue;
                BlokKljuc bk = new BlokKljuc(pred.getRedniBroj(), profId, pred.getNaziv(), t.dan());
                blokovi.computeIfAbsent(bk, k -> new ArrayList<>()).add(t);
            }
        }

        // 6. Sortiraj blokove deterministicno: po redniBroj predmeta, pa po danu, pa po prvom casu.
        List<Map.Entry<BlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>>> sortiraniBlokovi =
                new ArrayList<>(blokovi.entrySet());
        sortiraniBlokovi.sort((a, b) -> {
            int cmp = Short.compare(a.getKey().redniBrojPredmeta(), b.getKey().redniBrojPredmeta());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.getKey().dan().ordinal(), b.getKey().dan().ordinal());
            if (cmp != 0) return cmp;
            Short prvi = a.getValue().get(0).cas();
            Short drugi = b.getValue().get(0).cas();
            return Short.compare(prvi, drugi);
        });

        int brojBlokovaNedeljno = sortiraniBlokovi.size();

        // 7. Generisi dodele — po nedelji, blok dobija grupu (i + w × B) mod G + 1
        List<RotDodela> dodele = new ArrayList<>();
        for (short w = 1; w <= n; w++) {
            int shift = (w - 1) * brojBlokovaNedeljno;
            for (int i = 0; i < sortiraniBlokovi.size(); i++) {
                Map.Entry<BlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> blokEntry =
                        sortiraniBlokovi.get(i);
                BlokKljuc bk = blokEntry.getKey();
                int grupa = ((i + shift) % g) + 1;
                Korisnik profesor = profesoriMap.get(bk.profesorId());
                for (DetekcijaVezbiResponse.TerminVezbi t : blokEntry.getValue()) {
                    dodele.add(RotDodela.builder()
                            .rotacija(r)
                            .brojNedelje(w)
                            .dan(t.dan())
                            .cas(t.cas())
                            .profesor(profesor)
                            .predmetNaziv(bk.predmetNaziv())
                            .brojGrupe((short) grupa)
                            .build());
                }
            }
        }
        return dodele;
    }

    private record KljucTerminProfesor(Dan dan, Short cas, UUID profesorId) {}

    private record BlokKljuc(short redniBrojPredmeta, UUID profesorId, String predmetNaziv, Dan dan) {}
}
