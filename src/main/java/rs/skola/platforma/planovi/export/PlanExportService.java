package rs.skola.platforma.planovi.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
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
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.GodisnjiPlanTema;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generisanje Word (.docx) i PDF dokumenta za Godisnji plan po skolskom obrascu.
 * Word koristi Apache POI XWPF, PDF koristi OpenPDF (cisti Java, bez LibreOffice-a).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanExportService {

    private static final List<String> MESECI = List.of("IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI");

    private final SkolaRepository skolaRepository;

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

    /**
     * Tabela tema: jednostavna verzija sa 17 kolona (Rb, Oblast, 3 tipa casa, Ukupno,
     * Ishodi, 10 meseci). Nema spojenih header-celija — kompromisi za jednostavnost
     * generatora, ali svi podaci su prisutni.
     */
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

            String ishodiText = gpt.getTema() == null || gpt.getTema().getNaziv() == null
                    ? "" : ""; // ishodi se cuvaju per-tema u katalogu — prikaz u operativnom planu
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
        XWPFRun r = p.createRun();
        r.setText(text == null ? "" : text);
        r.setBold(bold);
        r.setFontSize(fontSize);
    }

    private void ukloniBorder(XWPFTable t) {
        t.getCTTbl().getTblPr().unsetTblBorders();
    }

    private static String sigurno(String s) {
        return s == null ? "" : s;
    }

    private static String broj(Short s) {
        return s == null || s == 0 ? "" : s.toString();
    }

    // ==================== PDF ====================

    /**
     * PDF generisanje kroz OpenPDF (samostalan Java PDF, bez ext. zavisnosti).
     * Sadrzaj prati istu strukturu kao Word — manje formatiran, vise tabelarno.
     */
    public byte[] generisiGodisnjiPlanPdf(GodisnjiPlan plan) {
        Skola skola = skolaRepository.findById(plan.getSkolaId()).orElse(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font naslovFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

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
                t.addCell(new Phrase("", normalFont));
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

    public static class ExportException extends BaseException {
        public ExportException(String message, Throwable cause) {
            super("EXPORT_GRESKA", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }
}
