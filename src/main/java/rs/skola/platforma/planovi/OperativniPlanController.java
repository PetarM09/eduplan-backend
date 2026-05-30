package rs.skola.platforma.planovi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.export.PlanStorageService;
import rs.skola.platforma.planovi.repo.OperativniPlanRepository;
import rs.skola.platforma.planovi.web.KreirajOperativniPlanRequest;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Operativni planovi", description = "Mesecni operativni planovi rada")
@RestController
@RequestMapping("/api/v1/planovi/operativni")
@RequiredArgsConstructor
public class OperativniPlanController {

    private final OperativniPlanService service;
    private final OperativniPlanRepository planRepo;
    private final PlanStorageService storageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Kreiraj ili azuriraj operativni plan (idempotentno po predmet+odeljenje+mesec+godina)")
    public ApiResponse<OperativniPlanResponse> kreiraj(@AuthenticationPrincipal CustomUserDetails ja,
                                                        @Valid @RequestBody KreirajOperativniPlanRequest req) {
        return ApiResponse.ok(service.kreirajIliAzuriraj(ja, req));
    }

    @PostMapping("/{id}/podnesi")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    public ApiResponse<OperativniPlanResponse> podnesi(@PathVariable UUID id,
                                                        @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.podnesi(id, ja));
    }

    @PostMapping("/{id}/kloniraj")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Klonira plan u drugu skolsku godinu (zadrzava sve stavke, briše evaluaciju)")
    public ApiResponse<OperativniPlanResponse> kloniraj(@PathVariable UUID id,
                                                         @RequestParam String novaSkolskaGodina,
                                                         @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.kloniraj(id, novaSkolskaGodina, ja));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    public ApiResponse<List<OperativniPlanResponse>> mojiPlanovi(@AuthenticationPrincipal CustomUserDetails ja,
                                                                  @RequestParam(required = false) Short mesec,
                                                                  @RequestParam(required = false) UUID predmetId,
                                                                  @RequestParam(required = false) String skolskaGodina) {
        return ApiResponse.ok(service.mojiPlanovi(ja, mesec, predmetId, skolskaGodina));
    }

    @GetMapping("/svi")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR','KOORDINATOR')")
    public ApiResponse<List<OperativniPlanResponse>> sviZaSkolu(
            @RequestParam(required = false) String skolskaGodina,
            @RequestParam(required = false) Short mesec,
            @RequestParam(required = false) UUID nastavnikId,
            @RequestParam(required = false) UUID predmetId,
            @RequestParam(required = false) UUID odeljenjeId,
            @RequestParam(required = false) PlanStatus status) {
        return ApiResponse.ok(service.sviZaSkolu(skolskaGodina, mesec, nastavnikId, predmetId, odeljenjeId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OperativniPlanResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise operativni plan (koordinator skole)")
    public void obrisi(@PathVariable UUID id) {
        service.obrisi(id);
    }

    @GetMapping("/{id}/download/word")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadWord(@PathVariable UUID id) {
        OperativniPlan plan = ucitajZaDownload(id);
        byte[] bytes = storageService.procitajOperativni(plan.getSkolaId(), plan.getId(), plan.getWordFajlPutanja());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"operativni-plan.docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    @GetMapping("/{id}/download/pdf")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        OperativniPlan plan = ucitajZaDownload(id);
        byte[] bytes = storageService.procitajOperativni(plan.getSkolaId(), plan.getId(), plan.getPdfFajlPutanja());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"operativni-plan.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    private OperativniPlan ucitajZaDownload(UUID id) {
        UUID skolaId = TenantContext.require();
        OperativniPlan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operativni plan", id));
        if (!skolaId.equals(plan.getSkolaId())) {
            throw new TenantViolationException();
        }
        return plan;
    }
}
