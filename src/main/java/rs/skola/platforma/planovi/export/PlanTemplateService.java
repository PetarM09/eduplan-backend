package rs.skola.platforma.planovi.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.skola.platforma.katalog.domain.Ishod;
import rs.skola.platforma.katalog.repo.IshodRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.GodisnjiPlanTema;
import rs.skola.platforma.planovi.domain.OpStavka;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;


import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanTemplateService {

    private static final List<String> MESECI_KEYEVI = List.of(
            "IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI");
    private static final DateTimeFormatter DAT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final SkolaRepository skolaRepo;
    private final IshodRepository ishodRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final WordTemplateEngine engine;
    private final WordToPdfConverter pdfConverter;

    // -------- GODISNJI --------

    public byte[] godisnjiWord(GodisnjiPlan plan) {
        Skola skola = skolaRepo.findById(plan.getSkolaId()).orElse(null);
        Map<String, Object> polja = new HashMap<>();
        polja.put("skolaNaziv", skola == null ? "" : sigurno(skola.getNaziv()));
        polja.put("skolaGrad", skola == null ? "" : sigurno(skola.getGrad()));
        polja.put("predmet", sigurno(plan.getPredmet().getNaziv()));
        polja.put("razred", plan.getRazred() == null ? "" : plan.getRazred().toString());
        polja.put("skolskaGodina", sigurno(plan.getSkolskaGodina()));
        polja.put("nastavnik", plan.getNastavnik().punoIme());
        polja.put("ciljeviZadaci", sigurno(plan.getCiljeviZadaci()));
        polja.put("udzebenik", sigurno(plan.getUdzebenik()));
        polja.put("autori", sigurno(plan.getAutori()));
        polja.put("literatura", sigurno(plan.getLiteratura()));
        polja.put("godisnjiFond", broj(plan.getGodisnjiFond()));
        polja.put("nedeljniFond", broj(plan.getNedeljniFond()));
        polja.put("odeljenja", odeljenjaLabel(plan));
        polja.put("dodatniRad", sigurno(plan.getDodatniRad()));
        polja.put("dopunskiRad", sigurno(plan.getDopunskiRad()));
        polja.put("napomene", sigurno(plan.getNapomene()));
        polja.put("datumPredaje", datumPredaje(plan));

        List<Map<String, Object>> teme = mapTeme(plan);
        Map<String, List<Map<String, Object>>> kolekcije = Map.of("tema", teme);

        try (InputStream is = ucitajSablon("templates/godisnji-plan.docx")) {
            return engine.popuni(is, polja, kolekcije);
        } catch (IOException ex) {
            throw new IllegalStateException("Ne mogu ucitati sablon godisnjeg plana", ex);
        }
    }

    public byte[] godisnjiPdf(GodisnjiPlan plan) {
        return pdfConverter.konvertuj(godisnjiWord(plan));
    }

    // -------- OPERATIVNI --------

    public byte[] operativniWord(OperativniPlan plan) {
        Skola skola = skolaRepo.findById(plan.getSkolaId()).orElse(null);
        Map<String, Object> polja = new HashMap<>();
        polja.put("skolaNaziv", skola == null ? "" : sigurno(skola.getNaziv()));
        polja.put("skolaGrad", skola == null ? "" : sigurno(skola.getGrad()));
        polja.put("mesec", nazivMeseca(plan.getMesec()));
        polja.put("predmet", sigurno(plan.getPredmet().getNaziv()));
        polja.put("razred", plan.getOdeljenje() == null || plan.getOdeljenje().getRazred() == null
                ? "" : plan.getOdeljenje().getRazred().toString());
        polja.put("skolskaGodina", sigurno(plan.getSkolskaGodina()));
        polja.put("nedeljniFond", broj(plan.getNedeljniFond()));
        polja.put("odeljenje", plan.getOdeljenje() == null ? "" : plan.getOdeljenje().label());
        polja.put("nastavnik", plan.getNastavnik().punoIme());
        polja.put("samoprocena", sigurno(plan.getSamoprocenaIshoda()));
        polja.put("datumPredaje", datumPredajeOp(plan));

        List<Map<String, Object>> stavke = mapStavke(plan);
        Map<String, List<Map<String, Object>>> kolekcije = Map.of("stavka", stavke);

        try (InputStream is = ucitajSablon("templates/operativni-plan.docx")) {
            return engine.popuni(is, polja, kolekcije);
        } catch (IOException ex) {
            throw new IllegalStateException("Ne mogu ucitati sablon operativnog plana", ex);
        }
    }

    public byte[] operativniPdf(OperativniPlan plan) {
        return pdfConverter.konvertuj(operativniWord(plan));
    }

    // -------- helpers --------

    private List<Map<String, Object>> mapTeme(GodisnjiPlan plan) {
        List<GodisnjiPlanTema> teme = plan.getTeme().stream()
                .sorted(Comparator.comparing(t -> t.getRedniBroj() == null ? Short.MAX_VALUE : t.getRedniBroj()))
                .toList();
        List<Map<String, Object>> rez = new java.util.ArrayList<>(teme.size());
        int implicitniRb = 1;
        for (GodisnjiPlanTema gpt : teme) {
            Map<String, Object> red = new LinkedHashMap<>();
            short rb = gpt.getRedniBroj() == null || gpt.getRedniBroj() == 0
                    ? (short) implicitniRb : gpt.getRedniBroj();
            red.put("rb", rb + ".");
            red.put("naziv", gpt.getTema() == null ? "" : sigurno(gpt.getTema().getNaziv()));
            red.put("obrada", broj(gpt.getCasObrada()));
            red.put("utvrd", broj(gpt.getCasUtvrd()));
            red.put("ostalo", broj(gpt.getCasOstalo()));
            red.put("ukupno", broj(gpt.getUkupnoCasova()));
            red.put("ishodi", ishodiZaTemu(plan.getSkolaId(), gpt));
            Map<String, Boolean> meseci = gpt.getMeseci() == null ? Map.of() : gpt.getMeseci();
            for (String m : MESECI_KEYEVI) {
                red.put("m" + m, Boolean.TRUE.equals(meseci.get(m)) ? "X" : "");
            }
            rez.add(red);
            implicitniRb++;
        }
        return rez;
    }

    private List<Map<String, Object>> mapStavke(OperativniPlan plan) {
        List<OpStavka> stavke = plan.getStavke().stream()
                .sorted(Comparator.comparing(OpStavka::getRedniBrojCasa))
                .toList();
        List<Map<String, Object>> rez = new java.util.ArrayList<>(stavke.size());
        for (OpStavka s : stavke) {
            Map<String, Object> red = new LinkedHashMap<>();
            red.put("tema", s.getTema() == null ? "" : sigurno(s.getTema().getNaziv()));
            red.put("ishodi", s.getIshodi().stream()
                    .map(i -> "• " + i.getOpis())
                    .collect(Collectors.joining("\n")));
            red.put("rbCasa", s.getRedniBrojCasa() == null ? "" : s.getRedniBrojCasa().toString());
            red.put("jedinica", s.getNastavnaJedinica() == null ? "" : sigurno(s.getNastavnaJedinica().getNaziv()));
            red.put("tipCasa", s.getTipCasa() == null ? "" : sigurno(s.getTipCasa().getNaziv()));
            red.put("metoda", s.getMetodaRada() == null ? "" : sigurno(s.getMetodaRada().getNaziv()));
            red.put("medjupredmetno", s.getMedjupredmetno().stream()
                    .map(mp -> mp.getPredmet().getNaziv()
                            + (mp.getOpisKompetencije() == null ? "" : ": " + mp.getOpisKompetencije()))
                    .collect(Collectors.joining("\n")));
            red.put("evaluacija", sigurno(s.getEvaluacija()));
            rez.add(red);
        }
        return rez;
    }

    private String ishodiZaTemu(java.util.UUID skolaId, GodisnjiPlanTema gpt) {
        if (gpt == null || gpt.getTema() == null || gpt.getTema().getId() == null) return "";
        List<Ishod> ishodi = ishodRepo
                .findAllBySkolaIdAndTema_IdOrderByCreatedAtAsc(skolaId, gpt.getTema().getId());
        if (ishodi.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Ishod i : ishodi) {
            if (i.getOpis() == null || i.getOpis().isBlank()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(i.getOpis().trim());
        }
        return sb.toString();
    }

    private String odeljenjaLabel(GodisnjiPlan plan) {
        if (plan.getOdeljenjaIds() == null || plan.getOdeljenjaIds().isEmpty()) return "";
        List<Odeljenje> odeljenja = odeljenjeRepo.findAllById(plan.getOdeljenjaIds());
        return odeljenja.stream()
                .sorted(Comparator.comparing(Odeljenje::getRazred)
                        .thenComparing(Odeljenje::getOznaka))
                .map(Odeljenje::label)
                .collect(Collectors.joining(", "));
    }

    private String datumPredaje(GodisnjiPlan plan) {
        return plan.getPodnetAt() != null
                ? plan.getPodnetAt().toLocalDate().format(DAT_FMT)
                : LocalDate.now().format(DAT_FMT);
    }

    private String datumPredajeOp(OperativniPlan plan) {
        return plan.getPodnetAt() != null
                ? plan.getPodnetAt().toLocalDate().format(DAT_FMT)
                : LocalDate.now().format(DAT_FMT);
    }

    private InputStream ucitajSablon(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        if (is == null) throw new IllegalStateException("Sablon nije pronadjen: " + resource);
        return is;
    }

    private static String sigurno(String s) {
        return s == null ? "" : s;
    }

    private static String broj(Short s) {
        return s == null || s == 0 ? "" : s.toString();
    }

    private static String nazivMeseca(Short m) {
        return switch (m == null ? 0 : m.intValue()) {
            case 1 -> "Januar"; case 2 -> "Februar"; case 3 -> "Mart"; case 4 -> "April";
            case 5 -> "Maj"; case 6 -> "Jun"; case 7 -> "Jul"; case 8 -> "Avgust";
            case 9 -> "Septembar"; case 10 -> "Oktobar"; case 11 -> "Novembar"; case 12 -> "Decembar";
            default -> "";
        };
    }
}
