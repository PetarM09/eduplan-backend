package rs.skola.platforma.planovi.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import rs.skola.platforma.common.exception.BaseException;
import rs.skola.platforma.katalog.domain.Ishod;
import rs.skola.platforma.katalog.repo.IshodRepository;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.GodisnjiPlanTema;
import rs.skola.platforma.planovi.domain.OpStavka;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanExportService {

    private static final List<String> MESECI = List.of("IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI");

    private static volatile BaseFont BASE_REGULAR;
    private static volatile BaseFont BASE_BOLD;

    private final SkolaRepository skolaRepository;
    private final IshodRepository ishodRepository;

    private static BaseFont baseFont(boolean bold) {
        BaseFont cache = bold ? BASE_BOLD : BASE_REGULAR;
        if (cache != null) return cache;
        synchronized (PlanExportService.class) {
            cache = bold ? BASE_BOLD : BASE_REGULAR;
            if (cache != null) return cache;
            String resource = bold ? "fonts/DejaVuSans-Bold.ttf" : "fonts/DejaVuSans.ttf";
            try (var is = PlanExportService.class.getClassLoader().getResourceAsStream(resource)) {
                if (is == null) {
                    throw new IllegalStateException("Font resource ne postoji: " + resource);
                }
                byte[] bytes = is.readAllBytes();
                BaseFont bf = BaseFont.createFont(
                        resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                if (bold) BASE_BOLD = bf; else BASE_REGULAR = bf;
                return bf;
            } catch (Exception ex) {
                throw new IllegalStateException("Ne mogu da ucitam font " + resource, ex);
            }
        }
    }

    private static Font pdfFont(boolean bold, float size, Color color) {
        return new Font(baseFont(bold), size, Font.NORMAL, color);
    }

    // ==================== WORD ====================

    public byte[] generisiGodisnjiPlanWord(GodisnjiPlan plan) {
        Skola skola = skolaRepository.findById(plan.getSkolaId()).orElse(null);
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            zaglavlje(doc, plan, skola);
            naslov(doc, "GODISNJI PLAN RADA");
            opstiPodaci(doc, plan);
            tabelaTema(doc, plan);
            footerPotpis(doc);

            doc.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Greska pri generisanju Word fajla: " + ex.getMessage(), ex);
        }
    }

    private void zaglavlje(XWPFDocument doc, GodisnjiPlan plan, Skola skola) {
        XWPFTable t = doc.createTable(1, 2);
        t.setWidth("100%");
        ukloniBorder(t);
        XWPFTableCell levo = t.getRow(0).getCell(0);
        XWPFTableCell desno = t.getRow(0).getCell(1);

        upisi(levo, skola == null ? "" : skola.getNaziv(), true, 14, ParagraphAlignment.LEFT);
        if (skola != null && skola.getGrad() != null) {
            upisi(levo, skola.getGrad(), false, 11, ParagraphAlignment.LEFT);
        }

        upisi(desno, "Predmet: " + sigurno(plan.getPredmet().getNaziv()), false, 11, ParagraphAlignment.LEFT);
        upisi(desno, "Razred: " + (plan.getRazred() == null ? "" : plan.getRazred().toString()), false, 11, ParagraphAlignment.LEFT);
        upisi(desno, "Sk. godina: " + sigurno(plan.getSkolskaGodina()), false, 11, ParagraphAlignment.LEFT);
        upisi(desno, "Nastavnik: " + plan.getNastavnik().punoIme(), false, 11, ParagraphAlignment.LEFT);
    }

    private void naslov(XWPFDocument doc, String text) {
        doc.createParagraph(); // razmak
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
        doc.createParagraph();
    }

    private void opstiPodaci(XWPFDocument doc, GodisnjiPlan plan) {
        String[][] redovi = {
                {"CILJEVI I ZADACI PREDMETA", sigurno(plan.getCiljeviZadaci())},
                {"PROPISANI UDZBENIK", sigurno(plan.getUdzebenik())},
                {"AUTORI", sigurno(plan.getAutori())},
                {"LITERATURA ZA REALIZACIJU PROGRAMA", sigurno(plan.getLiteratura())},
                {"GODISNJI FOND CASOVA", plan.getGodisnjiFond() == null ? "" : plan.getGodisnjiFond().toString()},
                {"NEDELJNI FOND CASOVA", plan.getNedeljniFond() == null ? "" : plan.getNedeljniFond().toString()},
                {"DODATNI RAD", sigurno(plan.getDodatniRad())},
                {"DOPUNSKI RAD", sigurno(plan.getDopunskiRad())}
        };
        XWPFTable t = doc.createTable(redovi.length, 2);
        t.setWidth("100%");
        for (int i = 0; i < redovi.length; i++) {
            upisi(t.getRow(i).getCell(0), redovi[i][0], true, 10, ParagraphAlignment.LEFT);
            upisi(t.getRow(i).getCell(1), redovi[i][1], false, 10, ParagraphAlignment.LEFT);
        }
        doc.createParagraph();
    }

    private void tabelaTema(XWPFDocument doc, GodisnjiPlan plan) {
        List<GodisnjiPlanTema> teme = plan.getTeme().stream()
                .sorted(Comparator.comparing(GodisnjiPlanTema::getRedniBroj))
                .toList();

        int kolona = 17;
        XWPFTable t = doc.createTable(teme.size() + 1, kolona);
        t.setWidth("100%");
        XWPFTableRow header = t.getRow(0);
        String[] hdr = {"R.br", "Oblast/Tema", "Obrada", "Utvrd.", "Ostalo", "Ukupno", "Ishodi",
                "IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI"};
        for (int i = 0; i < kolona; i++) {
            upisi(header.getCell(i), hdr[i], true, 9, ParagraphAlignment.CENTER);
        }

        for (int i = 0; i < teme.size(); i++) {
            GodisnjiPlanTema gpt = teme.get(i);
            XWPFTableRow row = t.getRow(i + 1);
            upisi(row.getCell(0), String.valueOf(gpt.getRedniBroj()), false, 9, ParagraphAlignment.CENTER);
            upisi(row.getCell(1), sigurno(gpt.getTema().getNaziv()), false, 9, ParagraphAlignment.LEFT);
            upisi(row.getCell(2), broj(gpt.getCasObrada()), false, 9, ParagraphAlignment.CENTER);
            upisi(row.getCell(3), broj(gpt.getCasUtvrd()), false, 9, ParagraphAlignment.CENTER);
            upisi(row.getCell(4), broj(gpt.getCasOstalo()), false, 9, ParagraphAlignment.CENTER);
            upisi(row.getCell(5), broj(gpt.getUkupnoCasova()), false, 9, ParagraphAlignment.CENTER);

            String ishodiText = ishodiZaTemu(plan.getSkolaId(), gpt);
            upisi(row.getCell(6), ishodiText, false, 9, ParagraphAlignment.LEFT);

            Map<String, Boolean> meseci = gpt.getMeseci() == null ? Map.of() : gpt.getMeseci();
            for (int m = 0; m < MESECI.size(); m++) {
                boolean predaje = Boolean.TRUE.equals(meseci.get(MESECI.get(m)));
                upisi(row.getCell(7 + m), predaje ? "X" : "", false, 9, ParagraphAlignment.CENTER);
            }
        }
        doc.createParagraph();
    }

    private void footerPotpis(XWPFDocument doc) {
        doc.createParagraph();
        doc.createParagraph();
        XWPFTable t = doc.createTable(1, 2);
        t.setWidth("100%");
        ukloniBorder(t);
        upisi(t.getRow(0).getCell(0), "Datum predaje:", true, 10, ParagraphAlignment.LEFT);
        upisi(t.getRow(0).getCell(1), "Potpis nastavnika:", true, 10, ParagraphAlignment.RIGHT);
    }

    private void upisi(XWPFTableCell cell, String text, boolean bold, int fontSize, ParagraphAlignment align) {
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(align);
        String safe = text == null ? "" : text;
        // Podrska za vise linija (npr. ishodi sa bullet-ima): split po \n, svaka
        // linija dobija svoj run + addBreak izmedju.
        String[] lines = safe.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                XWPFRun rb = p.createRun();
                rb.addBreak();
            }
            XWPFRun r = p.createRun();
            r.setText(lines[i]);
            r.setBold(bold);
            r.setFontSize(fontSize);
        }
    }

    private void ukloniBorder(XWPFTable t) {
        t.getCTTbl().getTblPr().unsetTblBorders();
    }

    private static String sigurno(String s) {
        return s == null ? "" : s;
    }

    private String ishodiZaTemu(java.util.UUID skolaId, GodisnjiPlanTema gpt) {
        if (gpt == null || gpt.getTema() == null || gpt.getTema().getId() == null) return "";
        List<Ishod> ishodi = ishodRepository
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

    private static String broj(Short s) {
        return s == null || s == 0 ? "" : s.toString();
    }

    // ==================== PDF ====================

    public byte[] generisiGodisnjiPlanPdf(GodisnjiPlan plan) {
        Skola skola = skolaRepository.findById(plan.getSkolaId()).orElse(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font naslovFont = pdfFont(true, 14, Color.BLACK);
            Font headerFont = pdfFont(true, 10, Color.BLACK);
            Font normalFont = pdfFont(false, 10, Color.BLACK);

            doc.add(new Paragraph(skola == null ? "" : skola.getNaziv(), naslovFont));
            if (skola != null && skola.getGrad() != null) {
                doc.add(new Paragraph(skola.getGrad(), normalFont));
            }
            doc.add(new Paragraph("GODISNJI PLAN RADA", naslovFont));
            doc.add(new Paragraph("Predmet: " + sigurno(plan.getPredmet().getNaziv()), normalFont));
            doc.add(new Paragraph("Razred: " + (plan.getRazred() == null ? "" : plan.getRazred()), normalFont));
            doc.add(new Paragraph("Skolska godina: " + sigurno(plan.getSkolskaGodina()), normalFont));
            doc.add(new Paragraph("Nastavnik: " + plan.getNastavnik().punoIme(), normalFont));
            doc.add(new Paragraph(" "));

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1f, 3f});
            dodajPar(info, "Ciljevi i zadaci", sigurno(plan.getCiljeviZadaci()), headerFont, normalFont);
            dodajPar(info, "Udzbenik", sigurno(plan.getUdzebenik()), headerFont, normalFont);
            dodajPar(info, "Autori", sigurno(plan.getAutori()), headerFont, normalFont);
            dodajPar(info, "Literatura", sigurno(plan.getLiteratura()), headerFont, normalFont);
            dodajPar(info, "Godisnji fond", plan.getGodisnjiFond() == null ? "" : plan.getGodisnjiFond().toString(),
                    headerFont, normalFont);
            dodajPar(info, "Nedeljni fond", plan.getNedeljniFond() == null ? "" : plan.getNedeljniFond().toString(),
                    headerFont, normalFont);
            dodajPar(info, "Dodatni rad", sigurno(plan.getDodatniRad()), headerFont, normalFont);
            dodajPar(info, "Dopunski rad", sigurno(plan.getDopunskiRad()), headerFont, normalFont);
            doc.add(info);
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(17);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{0.6f, 4f, 0.7f, 0.7f, 0.7f, 0.7f, 3f,
                    0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f});
            String[] hdr = {"R.br", "Oblast/Tema", "Obrada", "Utvrd.", "Ostalo", "Ukupno", "Ishodi",
                    "IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI"};
            for (String h : hdr) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                t.addCell(c);
            }
            List<GodisnjiPlanTema> teme = plan.getTeme().stream()
                    .sorted(Comparator.comparing(GodisnjiPlanTema::getRedniBroj))
                    .toList();
            for (GodisnjiPlanTema gpt : teme) {
                t.addCell(new Phrase(String.valueOf(gpt.getRedniBroj()), normalFont));
                t.addCell(new Phrase(sigurno(gpt.getTema().getNaziv()), normalFont));
                t.addCell(new Phrase(broj(gpt.getCasObrada()), normalFont));
                t.addCell(new Phrase(broj(gpt.getCasUtvrd()), normalFont));
                t.addCell(new Phrase(broj(gpt.getCasOstalo()), normalFont));
                t.addCell(new Phrase(broj(gpt.getUkupnoCasova()), normalFont));
                t.addCell(new Phrase(ishodiZaTemu(plan.getSkolaId(), gpt), normalFont));
                Map<String, Boolean> meseci = gpt.getMeseci() == null ? Map.of() : gpt.getMeseci();
                for (String m : MESECI) {
                    boolean predaje = Boolean.TRUE.equals(meseci.get(m));
                    PdfPCell mc = new PdfPCell(new Phrase(predaje ? "X" : "", normalFont));
                    mc.setHorizontalAlignment(Element.ALIGN_CENTER);
                    t.addCell(mc);
                }
            }
            doc.add(t);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ExportException("Greska pri generisanju PDF fajla: " + ex.getMessage(), ex);
        }
    }

    private void dodajPar(PdfPTable t, String label, String value, Font lblFont, Font valFont) {
        t.addCell(new Phrase(label, lblFont));
        t.addCell(new Phrase(value, valFont));
    }

    // ==================== OPERATIVNI PLAN — WORD ====================

    public byte[] generisiOperativniPlanWord(OperativniPlan plan) {
        Skola skola = skolaRepository.findById(plan.getSkolaId()).orElse(null);
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleR = titleP.createRun();
            titleR.setBold(true);
            titleR.setFontSize(16);
            titleR.setText("OPERATIVNI PLAN RADA — " + nazivMeseca(plan.getMesec()));

            // Zaglavlje sa osnovnim podacima
            XWPFTable header = doc.createTable(2, 2);
            header.setWidth("100%");
            upisi(header.getRow(0).getCell(0), "Predmet: " + sigurno(plan.getPredmet().getNaziv()),
                    true, 11, ParagraphAlignment.LEFT);
            upisi(header.getRow(0).getCell(1), "Skolska godina: " + sigurno(plan.getSkolskaGodina()),
                    true, 11, ParagraphAlignment.LEFT);
            upisi(header.getRow(1).getCell(0), "Odeljenje: " + plan.getOdeljenje().label(),
                    true, 11, ParagraphAlignment.LEFT);
            upisi(header.getRow(1).getCell(1),
                    "Nedeljni fond casova: " + (plan.getNedeljniFond() == null ? "-" : plan.getNedeljniFond()),
                    true, 11, ParagraphAlignment.LEFT);
            doc.createParagraph();

            // Glavna tabela: 8 kolona kao u skolskom obrascu
            List<OpStavka> stavke = plan.getStavke().stream()
                    .sorted(Comparator.comparing(OpStavka::getRedniBrojCasa))
                    .toList();
            int kol = 8;
            XWPFTable t = doc.createTable(stavke.size() + 1, kol);
            t.setWidth("100%");
            String[] hdr = {"Br. i naziv teme", "Ishodi", "R.br.", "Nastavna jedinica",
                    "Tip casa", "Metode rada", "Medjupredmetno povezivanje", "Evaluacija"};
            for (int i = 0; i < kol; i++) {
                upisi(t.getRow(0).getCell(i), hdr[i], true, 9, ParagraphAlignment.CENTER);
            }
            for (int i = 0; i < stavke.size(); i++) {
                OpStavka s = stavke.get(i);
                XWPFTableRow row = t.getRow(i + 1);
                upisi(row.getCell(0), s.getTema() == null ? "" : s.getTema().getNaziv(),
                        false, 9, ParagraphAlignment.LEFT);
                upisi(row.getCell(1),
                        s.getIshodi().stream().map(io -> "• " + io.getOpis())
                                .reduce((a, b) -> a + "\n" + b).orElse(""),
                        false, 9, ParagraphAlignment.LEFT);
                upisi(row.getCell(2), String.valueOf(s.getRedniBrojCasa()),
                        false, 9, ParagraphAlignment.CENTER);
                upisi(row.getCell(3), s.getNastavnaJedinica() == null ? "" : s.getNastavnaJedinica().getNaziv(),
                        false, 9, ParagraphAlignment.LEFT);
                upisi(row.getCell(4), s.getTipCasa() == null ? "" : s.getTipCasa().getNaziv(),
                        false, 9, ParagraphAlignment.CENTER);
                upisi(row.getCell(5), s.getMetodaRada() == null ? "" : s.getMetodaRada().getNaziv(),
                        false, 9, ParagraphAlignment.LEFT);
                upisi(row.getCell(6),
                        s.getMedjupredmetno().stream()
                                .map(mp -> mp.getPredmet().getNaziv()
                                        + (mp.getOpisKompetencije() == null ? "" : ": " + mp.getOpisKompetencije()))
                                .reduce((a, b) -> a + "\n" + b).orElse(""),
                        false, 9, ParagraphAlignment.LEFT);
                upisi(row.getCell(7), sigurno(s.getEvaluacija()), false, 9, ParagraphAlignment.LEFT);
            }

            doc.createParagraph();
            XWPFParagraph samo = doc.createParagraph();
            XWPFRun samoR = samo.createRun();
            samoR.setBold(true);
            samoR.setText("Samoprocena ishoda: ");
            XWPFRun samoTxt = samo.createRun();
            samoTxt.setText(sigurno(plan.getSamoprocenaIshoda()));

            XWPFTable footer = doc.createTable(1, 2);
            footer.setWidth("100%");
            ukloniBorder(footer);
            upisi(footer.getRow(0).getCell(0), "Datum predaje:", true, 10, ParagraphAlignment.LEFT);
            upisi(footer.getRow(0).getCell(1),
                    "Predmetni nastavnik: " + plan.getNastavnik().punoIme(),
                    true, 10, ParagraphAlignment.RIGHT);

            doc.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Greska pri generisanju Word fajla operativnog plana: " + ex.getMessage(), ex);
        }
    }

    // ==================== OPERATIVNI PLAN — PDF ====================

    public byte[] generisiOperativniPlanPdf(OperativniPlan plan) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font naslovFont = pdfFont(true, 14, Color.BLACK);
            Font headerFont = pdfFont(true, 9, Color.BLACK);
            Font normalFont = pdfFont(false, 9, Color.BLACK);

            doc.add(new Paragraph("OPERATIVNI PLAN RADA — " + nazivMeseca(plan.getMesec()), naslovFont));
            doc.add(new Paragraph("Predmet: " + sigurno(plan.getPredmet().getNaziv())
                    + "   |   Odeljenje: " + plan.getOdeljenje().label()
                    + "   |   Sk. godina: " + plan.getSkolskaGodina(), normalFont));
            doc.add(new Paragraph("Nastavnik: " + plan.getNastavnik().punoIme()
                    + "   |   Nedeljni fond: "
                    + (plan.getNedeljniFond() == null ? "-" : plan.getNedeljniFond()), normalFont));
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(8);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{2.5f, 3f, 0.6f, 3f, 1.2f, 1.5f, 2.5f, 2f});
            String[] hdr = {"Br. i naziv teme", "Ishodi", "R.br.", "Nastavna jedinica",
                    "Tip casa", "Metode rada", "Medjupredmetno", "Evaluacija"};
            for (String h : hdr) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                t.addCell(c);
            }
            List<OpStavka> stavke = plan.getStavke().stream()
                    .sorted(Comparator.comparing(OpStavka::getRedniBrojCasa))
                    .toList();
            for (OpStavka s : stavke) {
                t.addCell(new Phrase(s.getTema() == null ? "" : s.getTema().getNaziv(), normalFont));
                t.addCell(new Phrase(
                        s.getIshodi().stream().map(io -> "• " + io.getOpis())
                                .reduce((a, b) -> a + "\n" + b).orElse(""), normalFont));
                PdfPCell rb = new PdfPCell(new Phrase(String.valueOf(s.getRedniBrojCasa()), normalFont));
                rb.setHorizontalAlignment(Element.ALIGN_CENTER);
                t.addCell(rb);
                t.addCell(new Phrase(s.getNastavnaJedinica() == null ? "" : s.getNastavnaJedinica().getNaziv(), normalFont));
                t.addCell(new Phrase(s.getTipCasa() == null ? "" : s.getTipCasa().getNaziv(), normalFont));
                t.addCell(new Phrase(s.getMetodaRada() == null ? "" : s.getMetodaRada().getNaziv(), normalFont));
                t.addCell(new Phrase(
                        s.getMedjupredmetno().stream()
                                .map(mp -> mp.getPredmet().getNaziv()
                                        + (mp.getOpisKompetencije() == null ? "" : ": " + mp.getOpisKompetencije()))
                                .reduce((a, b) -> a + "\n" + b).orElse(""), normalFont));
                t.addCell(new Phrase(sigurno(s.getEvaluacija()), normalFont));
            }
            doc.add(t);

            if (plan.getSamoprocenaIshoda() != null && !plan.getSamoprocenaIshoda().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Samoprocena ishoda: " + plan.getSamoprocenaIshoda(), normalFont));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ExportException("Greska pri generisanju PDF fajla operativnog plana: " + ex.getMessage(), ex);
        }
    }

    private static String nazivMeseca(Short mesec) {
        return switch (mesec == null ? 0 : mesec.intValue()) {
            case 1 -> "Januar";
            case 2 -> "Februar";
            case 3 -> "Mart";
            case 4 -> "April";
            case 5 -> "Maj";
            case 6 -> "Jun";
            case 7 -> "Jul";
            case 8 -> "Avgust";
            case 9 -> "Septembar";
            case 10 -> "Oktobar";
            case 11 -> "Novembar";
            case 12 -> "Decembar";
            default -> "?";
        };
    }

    public static class ExportException extends BaseException {
        public ExportException(String message, Throwable cause) {
            super("EXPORT_GRESKA", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }
}
