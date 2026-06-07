package rs.skola.platforma.pozivnice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Poreklo;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.pozivnice.web.BootstrapRezultat;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PozivnicaExcelService {

    private final KorisnikRepository korisnikRepo;
    private final PredmetRepository predmetRepo;
    private final PozivnicaService pozivnicaService;

    @Transactional
    public BootstrapRezultat importuj(MultipartFile fajl) {
        UUID skolaId = TenantContext.require();
        List<Predmet> sviPredmeti = predmetRepo.findAllBySkolaIdOrderByRazredAscNazivAsc(skolaId);
        int nov = 0, presk = 0;
        List<String> upozorenja = new ArrayList<>();
        try (InputStream is = fajl.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int rowIdx = 0;
            for (Row row : sheet) {
                rowIdx++;
                if (rowIdx == 1) continue; // header
                String ime = strCell(row.getCell(0));
                String prezime = strCell(row.getCell(1));
                String email = strCell(row.getCell(2));
                String predmetiCsv = strCell(row.getCell(3));
                if (ime == null || ime.isBlank()) continue;
                if (korisnikRepo.findBySkolaIdAndImeIgnoreCaseAndPrezimeIgnoreCase(
                        skolaId, ime, prezime == null ? "" : prezime).isPresent()) {
                    presk++;
                    continue;
                }
                try {
                    Korisnik k = pozivnicaService.napraviPozvanog(
                            skolaId, ime, prezime, email, Poreklo.EXCEL);
                    Set<Predmet> vezani = new HashSet<>();
                    if (predmetiCsv != null && !predmetiCsv.isBlank()) {
                        for (String token : predmetiCsv.split("[,;]")) {
                            String naziv = token.trim();
                            if (naziv.isEmpty()) continue;
                            Predmet match = pronadji(sviPredmeti, naziv);
                            if (match != null) vezani.add(match);
                            else upozorenja.add("Red " + rowIdx + ": predmet \"" + naziv
                                    + "\" nije pronadjen u skolskom katalogu");
                        }
                    }
                    k.getPredmeti().addAll(vezani);
                    korisnikRepo.save(k);
                    nov++;
                } catch (Exception ex) {
                    upozorenja.add("Red " + rowIdx + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new ValidationException("Greska pri citanju fajla: " + ex.getMessage());
        }
        return new BootstrapRezultat(nov, presk, upozorenja);
    }

    private Predmet pronadji(List<Predmet> svi, String naziv) {
        String norm = normalizuj(naziv);
        for (Predmet p : svi) {
            if (normalizuj(p.getNaziv()).equals(norm)) return p;
        }
        return null;
    }

    private static String strCell(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf((long) c.getNumericCellValue());
        return null;
    }

    private static String normalizuj(String s) {
        if (s == null) return "";
        String dec = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return dec.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
