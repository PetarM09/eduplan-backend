package rs.skola.platforma.rotacija;

import org.springframework.stereotype.Component;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.raspored.domain.Dan;
import rs.skola.platforma.rotacija.domain.RotDodela;
import rs.skola.platforma.rotacija.domain.RotPredmet;
import rs.skola.platforma.rotacija.domain.Rotacija;
import rs.skola.platforma.rotacija.web.DetekcijaVezbiResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Algoritam dodele grupa na termine vezbi sa segmentacijom u chunkove od po 3 casa.
 *
 * Pravila:
 *   1. Termini istog profesora i istog predmeta u istom danu uzastopno se grupisu.
 *      Ako profesor tog dana ima vise od {@link #TRAJANJE_BLOKA} casova u nizu, niz
 *      se sece na chunkove od 3 casa — svaki chunk ide drugoj grupi (npr. 6 casova
 *      Pon 1-6 = 2 chunk-a × 3 casa kojima ce biti dodeljene razlicite grupe).
 *   2. Chunkovi se sortiraju po (dan, prvi cas, profesorId) — chunkovi koji se
 *      preklapaju u istom terminu (dan, cas) automatski imaju razlicite indekse,
 *      pa kroz linearni shift kroz nedelje ne mogu dobiti istu grupu.
 *   3. Grupa se rotira kroz nedelje: grupa = ((i + w − 1) mod G) + 1.
 *      Ravnomerna raspodela kroz ciklus.
 *
 * Validacija pre generisanja:
 *   - Broj grupa mora biti >= maksimalan broj profesora u jednom terminu —
 *     inace dva profesora u istom casu nuzno dobijaju istu grupu (konflikt).
 */
@Component
public class RotacijaAlgoritam {

    /** Standardno trajanje jednog bloka vezbi u jednom danu. */
    private static final int TRAJANJE_BLOKA = 3;

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

        // 4. Validacija: brojGrupa >= max profesora u terminu (samo ukljucenih)
        int maxProfesoraUTerminu = 0;
        for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
            int ukljuceni = 0;
            for (UUID profId : t.profesoriIds()) {
                if (profId != null && profesoriMap.containsKey(profId)) ukljuceni++;
            }
            maxProfesoraUTerminu = Math.max(maxProfesoraUTerminu, ukljuceni);
        }
        if (maxProfesoraUTerminu > g) {
            throw new ValidationException(
                    "MALO_GRUPA",
                    "U nekom terminu istovremeno predaje %d profesora, a broj grupa je %d. " +
                            "Povecaj broj grupa ili iskljuci nekog profesora."
                            .formatted(maxProfesoraUTerminu, g));
        }

        // 5. Po profesoru, distribuiraj predmete po terminima — sekvencijalno.
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

        // 6. Grupisi termine u "raw blokove" = uzastopni casovi istog profesora,
        //    istog predmeta u istom danu (par uzastopnih casova bez praznine).
        Map<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> rawBlokovi = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoProfesoru.entrySet()) {
            UUID profId = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            RawBlokKljuc trenutni = null;
            short ocekivaniCas = -1;
            for (DetekcijaVezbiResponse.TerminVezbi t : termini) {
                RotPredmet pred = terminPredmet.get(new KljucTerminProfesor(t.dan(), t.cas(), profId));
                if (pred == null) continue;
                // Novi raw blok ako se promenio dan / predmet ili nije uzastopan cas
                boolean nastavakBloka = trenutni != null
                        && trenutni.dan() == t.dan()
                        && trenutni.predmetNaziv().equals(pred.getNaziv())
                        && t.cas() == ocekivaniCas;
                if (!nastavakBloka) {
                    trenutni = new RawBlokKljuc(profId, pred.getNaziv(), t.dan(),
                            t.cas(), pred.getRedniBroj());
                }
                rawBlokovi.computeIfAbsent(trenutni, k -> new ArrayList<>()).add(t);
                ocekivaniCas = (short) (t.cas() + 1);
            }
        }

        // 7. Sece svaki raw blok na chunkove od TRAJANJE_BLOKA casova
        List<Chunk> chunkovi = new ArrayList<>();
        for (Map.Entry<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> e : rawBlokovi.entrySet()) {
            RawBlokKljuc bk = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            Korisnik profesor = profesoriMap.get(bk.profesorId());
            for (int start = 0; start < termini.size(); start += TRAJANJE_BLOKA) {
                int end = Math.min(start + TRAJANJE_BLOKA, termini.size());
                chunkovi.add(new Chunk(bk.profesorId(), bk.predmetNaziv(), profesor, bk.redniBrojPredmeta(),
                        new ArrayList<>(termini.subList(start, end))));
            }
        }

        // 8. Sortiraj chunkove po (dan, prvi cas, profesorId) — chunkovi koji se
        //    preklapaju u istom terminu (dan, cas) ce imati indekse koji se razlikuju
        //    za broj profesora u tom terminu — < G, pa razlicite grupe.
        chunkovi.sort((a, b) -> {
            DetekcijaVezbiResponse.TerminVezbi taPrvi = a.termini().get(0);
            DetekcijaVezbiResponse.TerminVezbi tbPrvi = b.termini().get(0);
            int cmp = Integer.compare(taPrvi.dan().ordinal(), tbPrvi.dan().ordinal());
            if (cmp != 0) return cmp;
            cmp = Short.compare(taPrvi.cas(), tbPrvi.cas());
            if (cmp != 0) return cmp;
            return a.profesorId().compareTo(b.profesorId());
        });

        // 9. Generisi dodele sa linearnim shift-om: grupa = ((i + w-1) mod G) + 1
        List<RotDodela> dodele = new ArrayList<>();
        for (short w = 1; w <= n; w++) {
            int shift = w - 1;
            // Cuvamo koja je grupa vec dodeljena u (dan, cas) ove nedelje — radi
            // sanity provere; jer sortiranje vec garantuje razlicitost, ali bezbedonosno.
            Map<TerminKljuc, Set<Short>> grupeUTerminu = new HashMap<>();
            for (int i = 0; i < chunkovi.size(); i++) {
                Chunk c = chunkovi.get(i);
                int grupa = ((i + shift) % g) + 1;
                for (DetekcijaVezbiResponse.TerminVezbi t : c.termini()) {
                    TerminKljuc tk = new TerminKljuc(t.dan(), t.cas());
                    Set<Short> postojece = grupeUTerminu.computeIfAbsent(tk, k -> new HashSet<>());
                    if (postojece.contains((short) grupa)) {
                        // Konflikt — pomeri grupu na sledecu slobodnu.
                        for (int probaj = 1; probaj < g; probaj++) {
                            short alt = (short) (((grupa - 1 + probaj) % g) + 1);
                            if (!postojece.contains(alt)) {
                                grupa = alt;
                                break;
                            }
                        }
                    }
                    postojece.add((short) grupa);
                    dodele.add(RotDodela.builder()
                            .rotacija(r)
                            .brojNedelje(w)
                            .dan(t.dan())
                            .cas(t.cas())
                            .profesor(c.profesor())
                            .predmetNaziv(c.predmetNaziv())
                            .brojGrupe((short) grupa)
                            .build());
                }
            }
        }
        return dodele;
    }

    private record KljucTerminProfesor(Dan dan, Short cas, UUID profesorId) {}

    private record TerminKljuc(Dan dan, Short cas) {}

    private record RawBlokKljuc(UUID profesorId, String predmetNaziv, Dan dan,
                                 Short prviCas, Short redniBrojPredmeta) {}

    private record Chunk(UUID profesorId, String predmetNaziv, Korisnik profesor,
                          Short redniBrojPredmeta, List<DetekcijaVezbiResponse.TerminVezbi> termini) {}
}
