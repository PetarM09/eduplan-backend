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
 * Algoritam dodele grupa na termine vezbi sa chunkovima od po 3 casa i
 * per-profesor rotacijom.
 *
 * Pravila:
 *   1. Termini istog profesora i istog predmeta u istom danu uzastopno se
 *      grupisu u "raw blok". Ako profesor tog dana ima vise od 3 casa zaredom,
 *      blok se sece na chunkove po {@link #TRAJANJE_BLOKA} casova.
 *   2. Chunkovi istog profesora dobijaju razlicite grupe — broji se indeks
 *      UNUTAR profesora (0, 1, 2, ...). Sa offset-om po profesoru (profOffset)
 *      i shift-om po nedelji, dobija se per-profesor rotacija koja istovremeno
 *      izbegava konflikte u istom terminu (dva profesora, ista grupa).
 *   3. grupa = ((indeksUnutarProf + profOffset + (w − 1)) mod G) + 1.
 *      Ako ipak dodje do konflikta u istom (dan, cas) zbog kombinacije, grupa
 *      chunk-a se pomera na sledecu slobodnu.
 *
 * Validacija:
 *   - brojGrupa >= max broj profesora u jednom terminu (ukljucenih u rotaciju).
 */
@Component
public class RotacijaAlgoritam {

    /** Standardno trajanje jednog bloka vezbi u jednom danu (casova zaredom). */
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

        // 3. Termini po profesoru — samo ukljuceni
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

        // 4. Validacija: brojGrupa >= max ukljucenih profesora u terminu
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
                    "U nekom terminu istovremeno predaje %d profesora, a broj grupa je %d. "
                            .formatted(maxProfesoraUTerminu, g)
                            + "Povecaj broj grupa ili iskljuci nekog profesora.");
        }

        // 5. Distribuiraj predmete po terminima (po profesoru, sekvencijalno)
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

        // 6. Raw blokovi = uzastopni casovi istog profesora, istog predmeta u istom danu
        Map<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> rawBlokovi = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoProfesoru.entrySet()) {
            UUID profId = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            RawBlokKljuc trenutni = null;
            short ocekivaniCas = -1;
            for (DetekcijaVezbiResponse.TerminVezbi t : termini) {
                RotPredmet pred = terminPredmet.get(new KljucTerminProfesor(t.dan(), t.cas(), profId));
                if (pred == null) continue;
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

        // 7. Sece raw blokove na chunkove po TRAJANJE_BLOKA casova
        List<Chunk> sviChunkovi = new ArrayList<>();
        for (Map.Entry<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> e : rawBlokovi.entrySet()) {
            RawBlokKljuc bk = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            Korisnik profesor = profesoriMap.get(bk.profesorId());
            for (int start = 0; start < termini.size(); start += TRAJANJE_BLOKA) {
                int end = Math.min(start + TRAJANJE_BLOKA, termini.size());
                sviChunkovi.add(new Chunk(bk.profesorId(), bk.predmetNaziv(), profesor,
                        new ArrayList<>(termini.subList(start, end))));
            }
        }

        // 8. Sortiraj profesore stabilno (po imenu, pa po id-u) — offset razlikuje profesore
        List<UUID> profIds = new ArrayList<>(profesoriMap.keySet());
        profIds.sort((a, b) -> {
            int cmp = profesoriMap.get(a).punoIme().compareTo(profesoriMap.get(b).punoIme());
            if (cmp != 0) return cmp;
            return a.compareTo(b);
        });
        Map<UUID, Integer> profOffset = new HashMap<>();
        for (int i = 0; i < profIds.size(); i++) {
            profOffset.put(profIds.get(i), i);
        }

        // 9. Grupisi chunkove po profesoru, sortiraj svaki po (dan, prvi cas)
        Map<UUID, List<Chunk>> chunkoviPoProfesoru = new LinkedHashMap<>();
        for (UUID profId : profIds) {
            chunkoviPoProfesoru.put(profId, new ArrayList<>());
        }
        for (Chunk c : sviChunkovi) {
            chunkoviPoProfesoru.get(c.profesorId()).add(c);
        }
        for (List<Chunk> lista : chunkoviPoProfesoru.values()) {
            lista.sort(Comparator
                    .comparing((Chunk c) -> c.termini().get(0).dan().ordinal())
                    .thenComparing(c -> c.termini().get(0).cas()));
        }

        // 10. Generisi dodele — grupa = ((indeksUnutarProf + profOffset + (w−1)) mod G) + 1
        List<RotDodela> dodele = new ArrayList<>();
        for (short w = 1; w <= n; w++) {
            int weekShift = w - 1;
            Map<TerminKljuc, Set<Short>> grupeUTerminu = new HashMap<>();

            for (UUID profId : profIds) {
                int pOffset = profOffset.get(profId);
                List<Chunk> profChunks = chunkoviPoProfesoru.get(profId);
                for (int idx = 0; idx < profChunks.size(); idx++) {
                    Chunk c = profChunks.get(idx);
                    int grupa = ((idx + pOffset + weekShift) % g) + 1;

                    // Konflikt: ako grupa vec postoji u BILO kom terminu ovog chunka
                    if (grupaKonfliktuje(c, grupa, grupeUTerminu)) {
                        for (int probaj = 1; probaj < g; probaj++) {
                            int alt = ((grupa - 1 + probaj) % g) + 1;
                            if (!grupaKonfliktuje(c, alt, grupeUTerminu)) {
                                grupa = alt;
                                break;
                            }
                        }
                    }

                    short finalGrupa = (short) grupa;
                    for (DetekcijaVezbiResponse.TerminVezbi t : c.termini()) {
                        grupeUTerminu
                                .computeIfAbsent(new TerminKljuc(t.dan(), t.cas()), k -> new HashSet<>())
                                .add(finalGrupa);
                        dodele.add(RotDodela.builder()
                                .rotacija(r)
                                .brojNedelje(w)
                                .dan(t.dan())
                                .cas(t.cas())
                                .profesor(c.profesor())
                                .predmetNaziv(c.predmetNaziv())
                                .brojGrupe(finalGrupa)
                                .build());
                    }
                }
            }
        }
        return dodele;
    }

    private boolean grupaKonfliktuje(Chunk c, int grupa, Map<TerminKljuc, Set<Short>> grupeUTerminu) {
        for (DetekcijaVezbiResponse.TerminVezbi t : c.termini()) {
            Set<Short> postojece = grupeUTerminu.get(new TerminKljuc(t.dan(), t.cas()));
            if (postojece != null && postojece.contains((short) grupa)) return true;
        }
        return false;
    }

    private record KljucTerminProfesor(Dan dan, Short cas, UUID profesorId) {}

    private record TerminKljuc(Dan dan, Short cas) {}

    private record RawBlokKljuc(UUID profesorId, String predmetNaziv, Dan dan,
                                 Short prviCas, Short redniBrojPredmeta) {}

    private record Chunk(UUID profesorId, String predmetNaziv, Korisnik profesor,
                          List<DetekcijaVezbiResponse.TerminVezbi> termini) {}
}
