package rs.skola.platforma.rotacija;

import org.springframework.stereotype.Component;
import rs.skola.platforma.raspored.domain.Dan;
import rs.skola.platforma.rotacija.domain.RotDodela;
import rs.skola.platforma.rotacija.domain.RotPredmet;
import rs.skola.platforma.rotacija.domain.Rotacija;
import rs.skola.platforma.rotacija.web.DetekcijaVezbiResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generise dodele grupa na termine vezbi kroz N nedelja.
 *
 * Algoritam:
 *   1. Za svakog profesora, distribuiramo njegove predmete preko njegovih termina
 *      vezbi iz rasporeda — prvih {@code casovaNedeljno} termina za prvi predmet,
 *      sledecih toliko za drugi, itd. (suma garantovano = broju termina po
 *      validaciji u servisu).
 *   2. Globalni redosled "kanala" (profesor+predmet) odredjuje pocetnu poziciju
 *      grupe u svakom terminu. U svakom terminu su aktivni samo oni kanali ciji
 *      profesor predaje u tom terminu.
 *   3. Kroz nedelje, grupe se rotiraju ciklicno (+1 svake nedelje).
 *
 * Slozenost: O(N × T × M) gde je T broj termina vezbi, M prosecan broj predmeta
 * u terminu — sve linearno za realne dimenzije (N ≤ 52, T ≤ 60, M ≤ 10).
 */
@Component
public class RotacijaAlgoritam {

    public List<RotDodela> generisi(Rotacija r, DetekcijaVezbiResponse detekcija) {
        int g = r.getBrojGrupa();
        int n = r.getBrojNedelja();

        // 1. Termini po profesoru (vec sortirani iz detekcije po dan/cas).
        Map<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> terminePoProfesoru = new HashMap<>();
        for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
            for (UUID profId : t.profesoriIds()) {
                terminePoProfesoru.computeIfAbsent(profId, k -> new ArrayList<>()).add(t);
            }
        }

        // 2. Predmeti po profesoru, sortirani po redniBroj.
        Map<UUID, List<RotPredmet>> predmetiPoProfesoru = new HashMap<>();
        for (RotPredmet p : r.getPredmeti()) {
            predmetiPoProfesoru
                    .computeIfAbsent(p.getProfesor().getId(), k -> new ArrayList<>())
                    .add(p);
        }
        predmetiPoProfesoru.values().forEach(lista -> lista.sort(Comparator.comparing(RotPredmet::getRedniBroj)));

        // 3. Mapiraj svaki (termin, profesor) na konkretni predmet tog termina.
        Map<KljucTerminProfesor, RotPredmet> terminProfPredmet = new HashMap<>();
        for (Map.Entry<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoProfesoru.entrySet()) {
            UUID profId = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            List<RotPredmet> predmeti = predmetiPoProfesoru.getOrDefault(profId, List.of());
            int idx = 0;
            for (RotPredmet pred : predmeti) {
                for (int i = 0; i < pred.getCasovaNedeljno() && idx < termini.size(); i++, idx++) {
                    DetekcijaVezbiResponse.TerminVezbi t = termini.get(idx);
                    terminProfPredmet.put(new KljucTerminProfesor(t.dan(), t.cas(), profId), pred);
                }
            }
        }

        // 4. Redosled svih kanala (za potencijalno globalno pomeranje).
        List<RotPredmet> sviKanali = new ArrayList<>(r.getPredmeti());
        sviKanali.sort(Comparator.comparing(RotPredmet::getRedniBroj));

        // 5. Po nedelji generisi dodele.
        List<RotDodela> dodele = new ArrayList<>();
        for (short w = 1; w <= n; w++) {
            int pomeranje = w - 1;
            for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
                List<RotPredmet> aktivniKanali = new ArrayList<>();
                for (UUID profId : t.profesoriIds()) {
                    RotPredmet pred = terminProfPredmet.get(new KljucTerminProfesor(t.dan(), t.cas(), profId));
                    if (pred != null) aktivniKanali.add(pred);
                }
                aktivniKanali.sort(Comparator.comparing(RotPredmet::getRedniBroj));

                int aktivnoBrojKanala = aktivniKanali.size();
                for (int j = 0; j < aktivnoBrojKanala; j++) {
                    if (j >= g) break; // vise profesora od grupa — visak preskace
                    RotPredmet kanal = aktivniKanali.get(j);
                    int grupa = ((j + pomeranje) % g) + 1;
                    dodele.add(RotDodela.builder()
                            .rotacija(r)
                            .brojNedelje(w)
                            .dan(kanal == null ? null : t.dan())
                            .cas(t.cas())
                            .profesor(kanal.getProfesor())
                            .predmetNaziv(kanal.getNaziv())
                            .brojGrupe((short) grupa)
                            .build());
                }
            }
        }
        return dodele;
    }

    private record KljucTerminProfesor(Dan dan, Short cas, UUID profesorId) {}
}
