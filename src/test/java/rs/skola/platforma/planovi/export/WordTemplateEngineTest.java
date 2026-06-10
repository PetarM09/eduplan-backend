package rs.skola.platforma.planovi.export;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WordTemplateEngineTest {

    private final WordTemplateEngine engine = new WordTemplateEngine();

    private Map<String, Object> tema(String rb, String naziv, String obrada, String utvrd,
                                       String ostalo, String ukupno, String ishodi,
                                       List<String> meseciKljucevi) {
        Map<String, Object> red = new java.util.LinkedHashMap<>();
        red.put("rb", rb); red.put("naziv", naziv);
        red.put("obrada", obrada); red.put("utvrd", utvrd);
        red.put("ostalo", ostalo); red.put("ukupno", ukupno);
        red.put("ishodi", ishodi);
        for (String m : List.of("IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI")) {
            red.put("m" + m, meseciKljucevi.contains(m) ? "X" : "");
        }
        return red;
    }

    @Test
    void popunjavaPoljaIRasirujeRedoveZaGodisnji() throws Exception {
        Map<String, Object> polja = Map.ofEntries(
                Map.entry("skolaNaziv", "Tehnicka skola"),
                Map.entry("skolaGrad", "Mladenovac"),
                Map.entry("predmet", "Matematika"),
                Map.entry("razred", "3"),
                Map.entry("skolskaGodina", "2025/2026"),
                Map.entry("nastavnik", "Marko Petrovic"),
                Map.entry("ciljeviZadaci", "Razvoj logickog razmisljanja"),
                Map.entry("udzebenik", "Matematika 3"),
                Map.entry("autori", "P. Petrovic"),
                Map.entry("literatura", "Zbirka zadataka"),
                Map.entry("godisnjiFond", "108"),
                Map.entry("nedeljniFond", "3"),
                Map.entry("odeljenja", "3-1, 3-2"),
                Map.entry("dodatniRad", ""),
                Map.entry("dopunskiRad", ""),
                Map.entry("napomene", ""),
                Map.entry("datumPredaje", "10.06.2026.")
        );

        Map<String, List<Map<String, Object>>> kolekcije = Map.of(
                "tema", List.of(
                        tema("1.", "Brojevi", "8", "4", "0", "12", "Ucenik ce prepoznati...",
                                List.of("IX", "X")),
                        tema("2.", "Funkcije", "10", "6", "2", "18", "Ucenik ce razumeti...",
                                List.of("XI", "XII", "I", "II")),
                        tema("3.", "Geometrija", "12", "8", "0", "20", "Ucenik ce konstruisati...",
                                List.of("III", "IV", "V", "VI"))
                )
        );

        byte[] rezultat;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("templates/godisnji-plan.docx")) {
            rezultat = engine.popuni(is, polja, kolekcije);
        }

        assertThat(rezultat).isNotEmpty();
        Files.write(Path.of("/tmp/godisnji-test.docx"), rezultat);

        // Provera: tabela tema ima header + 3 reda (3 teme), ne 1
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(rezultat))) {
            XWPFTable tabelaTema = doc.getTables().get(2); // 0=zaglavlje, 1=opsti, 2=teme
            int brojRedova = tabelaTema.getRows().size();
            // Originalno: 3 header reda + 1 row-sablon = 4. Nakon popune: 3 + 3 = 6
            assertThat(brojRedova).isEqualTo(6);

            // Provera da poslednji red ima "3." kao prvu celiju
            XWPFTableRow poslednji = tabelaTema.getRow(brojRedova - 1);
            String prvaCelija = poslednji.getCell(0).getText();
            assertThat(prvaCelija).contains("3");
        }
    }

    @Test
    void popunjavaOperativni() throws Exception {
        Map<String, Object> polja = Map.of(
                "mesec", "Februar",
                "predmet", "Matematika",
                "razred", "3",
                "skolskaGodina", "2025/2026",
                "nedeljniFond", "3",
                "odeljenje", "3-2",
                "nastavnik", "Marko Petrovic",
                "samoprocena", "Ostvareni planirani ishodi.",
                "datumPredaje", "10.06.2026."
        );

        Map<String, List<Map<String, Object>>> kolekcije = Map.of(
                "stavka", List.of(
                        Map.ofEntries(
                                Map.entry("tema", "Funkcije"),
                                Map.entry("ishodi", "• Razumeti definiciju"),
                                Map.entry("rbCasa", "1"),
                                Map.entry("jedinica", "Definicija funkcije"),
                                Map.entry("tipCasa", "Obrada"),
                                Map.entry("metoda", "Frontalna"),
                                Map.entry("medjupredmetno", ""),
                                Map.entry("evaluacija", "Usmeno")),
                        Map.ofEntries(
                                Map.entry("tema", "Funkcije"),
                                Map.entry("ishodi", "• Crtati graf"),
                                Map.entry("rbCasa", "2"),
                                Map.entry("jedinica", "Grafici funkcija"),
                                Map.entry("tipCasa", "Vezbe"),
                                Map.entry("metoda", "Grupni rad"),
                                Map.entry("medjupredmetno", "Informatika: koriscenje softvera"),
                                Map.entry("evaluacija", "Pismeno"))
                )
        );

        byte[] rezultat;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("templates/operativni-plan.docx")) {
            rezultat = engine.popuni(is, polja, kolekcije);
        }

        assertThat(rezultat).isNotEmpty();
        Files.write(Path.of("/tmp/operativni-test.docx"), rezultat);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(rezultat))) {
            XWPFTable tabela = doc.getTables().get(0);
            // header + 2 stavke = 3 reda
            assertThat(tabela.getRows().size()).isEqualTo(3);
        }
    }
}
