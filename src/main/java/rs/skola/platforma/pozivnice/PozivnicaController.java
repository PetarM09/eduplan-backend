package rs.skola.platforma.pozivnice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.pozivnice.web.AktivirajPozivnicuRequest;
import rs.skola.platforma.pozivnice.web.AzurirajEmailRequest;
import rs.skola.platforma.pozivnice.web.BootstrapRezultat;
import rs.skola.platforma.pozivnice.web.PostaviPredmeteRequest;
import rs.skola.platforma.pozivnice.web.PozivnicaInfoResponse;
import rs.skola.platforma.pozivnice.web.PozvaniKorisnikResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@Tag(name = "Pozivnice", description = "Bootstrap nastavnika iz XML rasporeda i Excel-a, magic-link aktivacija")
@RestController
@RequestMapping("/api/v1/pozivnice")
@RequiredArgsConstructor
public class PozivnicaController {

    private final PozivnicaService service;
    private final PozivnicaExcelService excelService;

    @GetMapping
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Lista POZVAN korisnika u skoli")
    public ApiResponse<List<PozvaniKorisnikResponse>> svi() {
        return ApiResponse.ok(service.svi());
    }

    @PostMapping("/bootstrap-iz-rasporeda")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Kreira POZVAN naloge za sve nemapirane nastavnike iz XML rasporeda")
    public ApiResponse<BootstrapRezultat> bootstrapIzRasporeda() {
        return ApiResponse.ok(service.bootstrapIzRasporeda());
    }

    @PostMapping(value = "/import-xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Uvozi nastavnike iz Excel sablona")
    public ApiResponse<BootstrapRezultat> importXlsx(@RequestParam("fajl") MultipartFile fajl) {
        return ApiResponse.ok(excelService.importuj(fajl));
    }

    @GetMapping(value = "/sablon-xlsx")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Preuzmi Excel sablon za uvoz nastavnika")
    public ResponseEntity<byte[]> sablonXlsx() {
        byte[] bytes = SablonGenerator.napravi();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pozivnice-sablon.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PutMapping("/{id}/predmeti")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Postavi predmete koje nastavnik predaje")
    public ApiResponse<PozvaniKorisnikResponse> postaviPredmete(@PathVariable UUID id,
                                                                  @Valid @RequestBody PostaviPredmeteRequest req) {
        return ApiResponse.ok(service.postaviPredmete(id, req));
    }

    @PutMapping("/{id}/email")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Azurira email POZVAN korisnika")
    public ApiResponse<PozvaniKorisnikResponse> azurirajEmail(@PathVariable UUID id,
                                                                @Valid @RequestBody AzurirajEmailRequest req) {
        return ApiResponse.ok(service.azurirajEmail(id, req));
    }

    @PostMapping("/{id}/posalji")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Salje mail sa magic-linkom korisniku")
    public void posaljiPozivnicu(@PathVariable UUID id) {
        service.posaljiPozivnicu(id);
    }

    // -------- PUBLIC: aktivacija --------

    @GetMapping("/info/{token}")
    @Operation(summary = "Public: pregled informacija o pozivnici")
    public ApiResponse<PozivnicaInfoResponse> info(@PathVariable UUID token) {
        return ApiResponse.ok(service.info(token));
    }

    @PostMapping("/aktiviraj/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Public: postavi sifru i aktivira nalog")
    public void aktiviraj(@PathVariable UUID token, @Valid @RequestBody AktivirajPozivnicuRequest req) {
        service.aktiviraj(token, req);
    }

    // -------- Sablon Excel generator --------

    private static final class SablonGenerator {
        static byte[] napravi() {
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                org.apache.poi.ss.usermodel.Sheet s = wb.createSheet("Nastavnici");
                org.apache.poi.ss.usermodel.Row h = s.createRow(0);
                h.createCell(0).setCellValue("Ime");
                h.createCell(1).setCellValue("Prezime");
                h.createCell(2).setCellValue("Email");
                h.createCell(3).setCellValue("Predmeti (zarez razdvojeni)");
                org.apache.poi.ss.usermodel.Row prim = s.createRow(1);
                prim.createCell(0).setCellValue("Marko");
                prim.createCell(1).setCellValue("Petrovic");
                prim.createCell(2).setCellValue("marko@skola.rs");
                prim.createCell(3).setCellValue("Matematika, Fizika");
                for (int i = 0; i < 4; i++) s.autoSizeColumn(i);
                wb.write(out);
                // Drzimo iznad i koristimo bytes
                byte[] data = out.toByteArray();
                try (ByteArrayInputStream ignored = new ByteArrayInputStream(data)) { /* warm */ }
                return data;
            } catch (Exception ex) {
                throw new RuntimeException("Greska pri pravljenju sablona", ex);
            }
        }
    }
}
