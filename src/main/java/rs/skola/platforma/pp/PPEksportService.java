package rs.skola.platforma.pp;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.BaseException;
import rs.skola.platforma.planovi.GodisnjiPlanService;
import rs.skola.platforma.planovi.OperativniPlanService;
import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;
import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.web.PPIzvestajResponse;
import rs.skola.platforma.pp.web.StatistikaResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Generise Excel (.xlsx) izvestaj za PP sluzbu: lista godisnjih planova, lista operativnih,
 * lista PP izvestaja i statistika — sve u zasebnim sheet-ovima.
 */
@Service
@RequiredArgsConstructor
public class PPEksportService {

    private final GodisnjiPlanService godisnjiService;
    private final OperativniPlanService operativniService;
    private final PPService ppService;
    private final StatistikaAggregatorService statistikaService;

    @Transactional(readOnly = true)
    public byte[] generisiExcel(String skolskaGodina) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(bold);

            // Sheet 1: Godisnji planovi
            Sheet g = wb.createSheet("Godisnji planovi");
            popuniZaglavlje(g, headerStyle, "Nastavnik", "Predmet", "Razred", "Skolska godina",
                    "Status", "Podnet", "Kreiran");
            int rb = 1;
            for (GodisnjiPlanResponse p : godisnjiService.sviZaSkolu(skolskaGodina, null)) {
                Row r = g.createRow(rb++);
                r.createCell(0).setCellValue(p.nastavnikIme());
                r.createCell(1).setCellValue(p.predmetNaziv());
                r.createCell(2).setCellValue(p.razred() == null ? "" : p.razred().toString());
                r.createCell(3).setCellValue(p.skolskaGodina());
                r.createCell(4).setCellValue(p.status().name());
                r.createCell(5).setCellValue(p.podnetAt() == null ? "" : p.podnetAt().toString());
                r.createCell(6).setCellValue(p.createdAt() == null ? "" : p.createdAt().toString());
            }

            // Sheet 2: Operativni planovi
            Sheet o = wb.createSheet("Operativni planovi");
            popuniZaglavlje(o, headerStyle, "Nastavnik", "Predmet", "Odeljenje", "Mesec",
                    "Skolska godina", "Status", "Podnet");
            rb = 1;
            for (OperativniPlanResponse p : operativniService.sviZaSkolu(
                    skolskaGodina, null, null, null, null, null)) {
                Row r = o.createRow(rb++);
                r.createCell(0).setCellValue(p.nastavnikIme());
                r.createCell(1).setCellValue(p.predmetNaziv());
                r.createCell(2).setCellValue(p.odeljenjeLabel());
                r.createCell(3).setCellValue(p.mesec());
                r.createCell(4).setCellValue(p.skolskaGodina());
                r.createCell(5).setCellValue(p.status().name());
                r.createCell(6).setCellValue(p.podnetAt() == null ? "" : p.podnetAt().toString());
            }

            // Sheet 3: PP izvestaji
            Sheet izv = wb.createSheet("PP izvestaji");
            popuniZaglavlje(izv, headerStyle, "Staresina", "Odeljenje", "Period",
                    "Skolska godina", "Status", "Podnet");
            rb = 1;
            for (PPIzvestajResponse i : ppService.sviZaSkolu(skolskaGodina, null, null)) {
                Row r = izv.createRow(rb++);
                r.createCell(0).setCellValue(i.staresinaIme());
                r.createCell(1).setCellValue(i.odeljenjeLabel());
                r.createCell(2).setCellValue(i.period().name());
                r.createCell(3).setCellValue(i.skolskaGodina());
                r.createCell(4).setCellValue(i.status().name());
                r.createCell(5).setCellValue(i.podnetAt() == null ? "" : i.podnetAt().toString());
            }

            // Sheet 4: Statistika sumirana po periodima
            Sheet stat = wb.createSheet("Statistika");
            int redIdx = 0;
            for (PPPeriod period : PPPeriod.values()) {
                StatistikaResponse s = statistikaService.agregiraj(skolskaGodina, period);
                Row title = stat.createRow(redIdx++);
                Cell cTitle = title.createCell(0);
                cTitle.setCellValue("Period: " + period.name()
                        + "   |   Izvestaja: " + s.brojIzvestaja());
                cTitle.setCellStyle(headerStyle);
                redIdx = dodaj(stat, redIdx, "Ukupno ucenika", s.ukupnoUcenika());
                redIdx = dodaj(stat, redIdx, "Muski", s.ucenikaMuski());
                redIdx = dodaj(stat, redIdx, "Zenski", s.ucenikaZenski());
                redIdx = dodaj(stat, redIdx, "Opravdane izostanci", s.prisustvo().opravdana());
                redIdx = dodaj(stat, redIdx, "Neopravdane izostanci", s.prisustvo().neopravdana());
                for (Map.Entry<String, Long> e : s.vladanjeDistribucija().entrySet()) {
                    redIdx = dodaj(stat, redIdx, "Vladanje: " + e.getKey(), e.getValue());
                }
                for (Map.Entry<String, Long> e : s.uspehDistribucija().entrySet()) {
                    redIdx = dodaj(stat, redIdx, "Uspeh: " + e.getKey(), e.getValue());
                }
                redIdx++; // prazan red
            }

            for (int i = 0; i < 4; i++) {
                Sheet sh = wb.getSheetAt(i);
                for (int c = 0; c < 7; c++) sh.autoSizeColumn(c);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Greska pri Excel eksportu: " + ex.getMessage(), ex);
        }
    }

    private void popuniZaglavlje(Sheet sheet, CellStyle style, String... naslovi) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < naslovi.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(naslovi[i]);
            c.setCellStyle(style);
        }
    }

    private int dodaj(Sheet s, int red, String label, long vrednost) {
        Row r = s.createRow(red);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(vrednost);
        return red + 1;
    }

    public static class ExportException extends BaseException {
        public ExportException(String message, Throwable cause) {
            super("EXPORT_GRESKA", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }
}
