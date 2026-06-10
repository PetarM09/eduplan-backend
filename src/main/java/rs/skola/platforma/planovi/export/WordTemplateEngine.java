package rs.skola.platforma.planovi.export;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mehanizam za popunjavanje .docx sablona. Dva tipa markera:
 *   {{polje}}                — zamena skalarnim vrednostima
 *   {{kolekcija.polje}}      — red tabele koji sadrzi ove markere se klonira
 *                              za svaku stavku kolekcije; original se brise.
 *
 * Markeri u tabelama radije se cesto razbijaju u vise <w:r> runs zbog
 * Word-ovog auto-formatiranja. Implementacija to resava tako sto se runs
 * u paragrafu konkateniraju u jedan string, zamene se markeri, pa se
 * tekst vraca u PRVI run i ostali se brisu (uz cuvanje stila prvog runa).
 */
@Slf4j
@Component
public class WordTemplateEngine {

    private static final Pattern MARKER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_.]*)\\s*}}");

    public byte[] popuni(InputStream template,
                          Map<String, Object> polja,
                          Map<String, List<Map<String, Object>>> kolekcije) {
        try (XWPFDocument doc = new XWPFDocument(template);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            obradiBody(doc, polja, kolekcije);
            doc.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Greska pri popunjavanju sablona", ex);
        }
    }

    private void obradiBody(XWPFDocument doc,
                             Map<String, Object> polja,
                             Map<String, List<Map<String, Object>>> kolekcije) {
        // Obrada top-level paragrafa
        for (IBodyElement el : doc.getBodyElements()) {
            if (el instanceof XWPFParagraph p) {
                zameniUParagrafu(p, polja);
            } else if (el instanceof XWPFTable t) {
                obradiTabelu(t, polja, kolekcije);
            }
        }
    }

    private void obradiTabelu(XWPFTable tabela,
                               Map<String, Object> polja,
                               Map<String, List<Map<String, Object>>> kolekcije) {
        List<XWPFTableRow> redovi = new ArrayList<>(tabela.getRows());
        for (XWPFTableRow row : redovi) {
            String kolekcija = pronadjiKolekcijuRedaSablona(row);
            if (kolekcija != null && kolekcije.containsKey(kolekcija)) {
                rasiriRedSablon(tabela, row, kolekcija, kolekcije.get(kolekcija));
            } else {
                // Obican red sa {{polje}} — direktna zamena
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        zameniUParagrafu(p, polja);
                    }
                    for (XWPFTable nested : cell.getTables()) {
                        obradiTabelu(nested, polja, kolekcije);
                    }
                }
            }
        }
    }

    /**
     * Vraca naziv kolekcije ako red sadrzi marker oblika {{kolekcija.polje}}.
     * Validira da SVI takvi markeri u redu koriste IS TU kolekciju (sprecava
     * mesanje grupa u jednom redu).
     */
    private String pronadjiKolekcijuRedaSablona(XWPFTableRow row) {
        String detektovana = null;
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                String tekst = sastaviTekstParagrafa(p);
                Matcher m = MARKER.matcher(tekst);
                while (m.find()) {
                    String ime = m.group(1);
                    int dot = ime.indexOf('.');
                    if (dot < 0) continue;
                    String kolekcija = ime.substring(0, dot);
                    if (detektovana == null) detektovana = kolekcija;
                    else if (!detektovana.equals(kolekcija)) {
                        log.warn("Red sadrzi markere razlicitih kolekcija: {} i {}", detektovana, kolekcija);
                    }
                }
            }
        }
        return detektovana;
    }

    private void rasiriRedSablon(XWPFTable tabela,
                                   XWPFTableRow sablon,
                                   String prefiks,
                                   List<Map<String, Object>> stavke) {
        int idxSablona = tabela.getRows().indexOf(sablon);
        if (idxSablona < 0) return;

        // Sacuvamo XML originalnog reda za klonovanje (sa svom formatacijom)
        CTRow xmlOriginala = (CTRow) sablon.getCtRow().copy();

        if (stavke == null || stavke.isEmpty()) {
            tabela.removeRow(idxSablona);
            return;
        }

        // Prvi item — popunimo postojeci sablon u-mestu (zadrzava se njegov
        // XML referenca u parentu, nema race-uslova sa internal POI cache-om).
        popuniRed(sablon, sStavkePrefiks(prefiks, stavke.get(0)));

        // Ostatak — pravimo nove redove i POPUNJAVAMO IH PRE INSERT-a.
        // POI-jev `addRow(row, pos)` interno radi `setTrArray(pos, row.getCtRow())`
        // sto je XmlBeans deep-copy: posle insert-a izmene nad lokalnim CTRow se
        // NE propagiraju u tabelu. Zato red mora biti finalizovan pre poziva.
        int insertAt = idxSablona + 1;
        for (int i = 1; i < stavke.size(); i++) {
            CTRow novXml = (CTRow) xmlOriginala.copy();
            XWPFTableRow privremeni = new XWPFTableRow(novXml, tabela);
            popuniRed(privremeni, sStavkePrefiks(prefiks, stavke.get(i)));
            boolean ok = tabela.addRow(privremeni, insertAt);
            if (!ok) {
                log.warn("Ne mogu da ubacim klon reda na poziciji {}", insertAt);
                break;
            }
            insertAt++;
        }
    }

    private void popuniRed(XWPFTableRow red, Map<String, Object> polja) {
        for (XWPFTableCell cell : red.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                zameniUParagrafu(p, polja);
            }
        }
    }

    private Map<String, Object> sStavkePrefiks(String prefiks, Map<String, Object> stavka) {
        Map<String, Object> rez = new HashMap<>();
        for (Map.Entry<String, Object> e : stavka.entrySet()) {
            rez.put(prefiks + "." + e.getKey(), e.getValue());
        }
        return rez;
    }

    /**
     * Konkatenira sve runs paragrafa, zameni markere, zatim upise rezultat
     * u prvi run i ukloni ostale (cuva style prvog runa). Visestruke linije
     * (newline u vrednosti) prevodi u break-ove unutar runa.
     */
    private void zameniUParagrafu(XWPFParagraph p, Map<String, Object> polja) {
        List<XWPFRun> runs = p.getRuns();
        if (runs.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : runs) {
            String t = r.text();
            if (t != null) sb.append(t);
        }
        String original = sb.toString();
        if (!original.contains("{{")) return;

        Matcher m = MARKER.matcher(original);
        StringBuilder out = new StringBuilder();
        int last = 0;
        boolean menjano = false;
        while (m.find()) {
            menjano = true;
            out.append(original, last, m.start());
            Object val = polja.get(m.group(1));
            out.append(val == null ? "" : String.valueOf(val));
            last = m.end();
        }
        out.append(original, last, original.length());
        if (!menjano) return;

        // Sacuvamo prvi run i obrisemo ostale (kraj prema pocetku)
        XWPFRun prvi = runs.get(0);
        for (int i = runs.size() - 1; i >= 1; i--) {
            p.removeRun(i);
        }
        // Ocistimo postojeci tekst u prvom runu
        prvi.setText("", 0);
        // Razbijemo po \n na vise text+break sekvenci
        String[] linije = out.toString().split("\n", -1);
        for (int i = 0; i < linije.length; i++) {
            if (i == 0) prvi.setText(linije[i], 0);
            else {
                prvi.addBreak();
                prvi.setText(linije[i]);
            }
        }
    }

    private String sastaviTekstParagrafa(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : p.getRuns()) {
            String t = r.text();
            if (t != null) sb.append(t);
        }
        return sb.toString();
    }
}
