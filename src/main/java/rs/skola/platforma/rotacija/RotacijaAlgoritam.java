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

@Component
public class RotacijaAlgoritam {

    private static final int TRAJANJE_BLOKA = 3;

    public List<RotDodela> generisi(Rotacija r, DetekcijaVezbiResponse detekcija) {
        int g = r.getBrojGrupa();
        int n = r.getBrojNedelja();

        // Skup labela ukljucenih profesora + mapa label -> Korisnik (null ako nije mapiran)
        Set<String> ukljuceniLabeli = new HashSet<>();
        Map<String, Korisnik> profesorPoLabelu = new HashMap<>();
        for (RotPredmet p : r.getPredmeti()) {
            ukljuceniLabeli.add(p.getProfesorLabel());
            if (p.getProfesor() != null) {
                profesorPoLabelu.putIfAbsent(p.getProfesorLabel(), p.getProfesor());
            }
        }

        Map<String, List<RotPredmet>> predmetiPoLabelu = new HashMap<>();
        for (RotPredmet p : r.getPredmeti()) {
            predmetiPoLabelu.computeIfAbsent(p.getProfesorLabel(), k -> new ArrayList<>()).add(p);
        }
        predmetiPoLabelu.values().forEach(lista -> lista.sort(Comparator.comparing(RotPredmet::getRedniBroj)));

        // Termini po profesoru — po labelu iz detekcije
        Map<String, List<DetekcijaVezbiResponse.TerminVezbi>> terminePoLabelu = new HashMap<>();
        for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
            for (String label : t.profesoriImena()) {
                if (!ukljuceniLabeli.contains(label)) continue;
                terminePoLabelu.computeIfAbsent(label, k -> new ArrayList<>()).add(t);
            }
        }
        terminePoLabelu.values().forEach(lista -> lista.sort(Comparator
                .comparing((DetekcijaVezbiResponse.TerminVezbi t) -> t.dan().ordinal())
                .thenComparing(DetekcijaVezbiResponse.TerminVezbi::cas)));

        int maxProfesoraUTerminu = 0;
        for (DetekcijaVezbiResponse.TerminVezbi t : detekcija.termini()) {
            int ukljuceni = 0;
            for (String label : t.profesoriImena()) {
                if (ukljuceniLabeli.contains(label)) ukljuceni++;
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

        Map<KljucTerminLabel, RotPredmet> terminPredmet = new HashMap<>();
        for (Map.Entry<String, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoLabelu.entrySet()) {
            String label = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            List<RotPredmet> predmeti = predmetiPoLabelu.getOrDefault(label, List.of());
            int idx = 0;
            for (RotPredmet pred : predmeti) {
                for (int i = 0; i < pred.getCasovaNedeljno() && idx < termini.size(); i++, idx++) {
                    DetekcijaVezbiResponse.TerminVezbi t = termini.get(idx);
                    terminPredmet.put(new KljucTerminLabel(t.dan(), t.cas(), label), pred);
                }
            }
        }

        Map<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> rawBlokovi = new LinkedHashMap<>();
        for (Map.Entry<String, List<DetekcijaVezbiResponse.TerminVezbi>> e : terminePoLabelu.entrySet()) {
            String label = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            RawBlokKljuc trenutni = null;
            short ocekivaniCas = -1;
            for (DetekcijaVezbiResponse.TerminVezbi t : termini) {
                RotPredmet pred = terminPredmet.get(new KljucTerminLabel(t.dan(), t.cas(), label));
                if (pred == null) continue;
                boolean nastavakBloka = trenutni != null
                        && trenutni.dan() == t.dan()
                        && trenutni.predmetNaziv().equals(pred.getNaziv())
                        && t.cas() == ocekivaniCas;
                if (!nastavakBloka) {
                    trenutni = new RawBlokKljuc(label, pred.getNaziv(), t.dan(),
                            t.cas(), pred.getRedniBroj());
                }
                rawBlokovi.computeIfAbsent(trenutni, k -> new ArrayList<>()).add(t);
                ocekivaniCas = (short) (t.cas() + 1);
            }
        }

        List<Chunk> sviChunkovi = new ArrayList<>();
        for (Map.Entry<RawBlokKljuc, List<DetekcijaVezbiResponse.TerminVezbi>> e : rawBlokovi.entrySet()) {
            RawBlokKljuc bk = e.getKey();
            List<DetekcijaVezbiResponse.TerminVezbi> termini = e.getValue();
            Korisnik profesor = profesorPoLabelu.get(bk.profesorLabel());
            for (int start = 0; start < termini.size(); start += TRAJANJE_BLOKA) {
                int end = Math.min(start + TRAJANJE_BLOKA, termini.size());
                sviChunkovi.add(new Chunk(bk.profesorLabel(), bk.predmetNaziv(), profesor,
                        new ArrayList<>(termini.subList(start, end))));
            }
        }

        List<String> profLabeli = new ArrayList<>(ukljuceniLabeli);
        profLabeli.sort(String::compareTo);
        Map<String, Integer> profOffset = new HashMap<>();
        for (int i = 0; i < profLabeli.size(); i++) {
            profOffset.put(profLabeli.get(i), i);
        }

        Map<String, List<Chunk>> chunkoviPoLabelu = new LinkedHashMap<>();
        for (String label : profLabeli) {
            chunkoviPoLabelu.put(label, new ArrayList<>());
        }
        for (Chunk c : sviChunkovi) {
            chunkoviPoLabelu.get(c.profesorLabel()).add(c);
        }
        for (List<Chunk> lista : chunkoviPoLabelu.values()) {
            lista.sort(Comparator
                    .comparing((Chunk c) -> c.termini().get(0).dan().ordinal())
                    .thenComparing(c -> c.termini().get(0).cas()));
        }

        List<RotDodela> dodele = new ArrayList<>();
        for (short w = 1; w <= n; w++) {
            int weekShift = w - 1;
            Map<TerminKljuc, Set<Short>> grupeUTerminu = new HashMap<>();

            for (String label : profLabeli) {
                int pOffset = profOffset.get(label);
                List<Chunk> profChunks = chunkoviPoLabelu.get(label);
                for (int idx = 0; idx < profChunks.size(); idx++) {
                    Chunk c = profChunks.get(idx);
                    int grupa = ((idx + pOffset + weekShift) % g) + 1;

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
                                .profesorLabel(c.profesorLabel())
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

    private record KljucTerminLabel(Dan dan, Short cas, String profesorLabel) {}

    private record TerminKljuc(Dan dan, Short cas) {}

    private record RawBlokKljuc(String profesorLabel, String predmetNaziv, Dan dan,
                                 Short prviCas, Short redniBrojPredmeta) {}

    private record Chunk(String profesorLabel, String predmetNaziv, Korisnik profesor,
                          List<DetekcijaVezbiResponse.TerminVezbi> termini) {}
}
