package rs.skola.platforma.planovi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.export.PlanStorageService;
import rs.skola.platforma.planovi.repo.GodisnjiPlanRepository;
import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.KreirajGodisnjiPlanRequest;

import java.util.List;
import java.util.UUID;

@Tag(name = "Godisnji planovi", description = "Globalni planovi rada — kreiranje, podnosenje, eksport, download")
@RestController
@RequestMapping("/api/v1/planovi/godisnji")
@RequiredArgsConstructor
public class GodisnjiPlanController {

    private final GodisnjiPlanService service;
    private final GodisnjiPlanRepository planRepo;
    private final PlanStorageService storageService;

    @PostMapping
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Kreiraj ili azuriraj plan (idempotentno po predmet+godina+nastavnik)")
    public ApiResponse<GodisnjiPlanResponse> kreiraj(@AuthenticationPrincipal CustomUserDetails ja,
                                                      @Valid @RequestBody KreirajGodisnjiPlanRequest req) {
        return ApiResponse.ok(service.kreirajIliAzuriraj(ja, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Azuriraj plan kroz isti payload — regenerise Word/PDF i salje mail")
    public ApiResponse<GodisnjiPlanResponse> azuriraj(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CustomUserDetails ja,
                                                      @Valid @RequestBody KreirajGodisnjiPlanRequest req) {
        // ID iz putanje se ignorise — service idempotentno gleda po predmetu+godini
        return ApiResponse.ok(service.kreirajIliAzuriraj(ja, req));
    }

    @PostMapping("/{id}/podnesi")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Promeni status iz NACRT u PODNET")
    public ApiResponse<GodisnjiPlanResponse> podnesi(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.podnesi(id, ja));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Moji godisnji planovi")
    public ApiResponse<List<GodisnjiPlanResponse>> mojiPlanovi(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.mojiPlanovi(ja));
    }

    @GetMapping("/svi")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "Svi planovi skole sa filterima")
    public ApiResponse<List<GodisnjiPlanResponse>> sviZaSkolu(
            @RequestParam(required = false) String skolskaGodina,
            @RequestParam(required = false) PlanStatus status) {
        return ApiResponse.ok(service.sviZaSkolu(skolskaGodina, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Pregled jednog plana sa svim temama")
    public ApiResponse<GodisnjiPlanResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @GetMapping("/{id}/download/word")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Operation(summary = "Download Word fajla plana (.docx)")
    public ResponseEntity<byte[]> downloadWord(@PathVariable UUID id) {
        GodisnjiPlan plan = ucitajZaDownload(id);
        byte[] bytes = storageService.procitaj(plan.getSkolaId(), plan.getId(), plan.getWordFajlPutanja());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"godisnji-plan.docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    @GetMapping("/{id}/download/pdf")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Operation(summary = "Download PDF fajla plana")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        GodisnjiPlan plan = ucitajZaDownload(id);
        byte[] bytes = storageService.procitaj(plan.getSkolaId(), plan.getId(), plan.getPdfFajlPutanja());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"godisnji-plan.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    private GodisnjiPlan ucitajZaDownload(UUID id) {
        UUID skolaId = TenantContext.require();
        GodisnjiPlan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Godisnji plan", id));
        if (!skolaId.equals(plan.getSkolaId())) {
            throw new TenantViolationException();
        }
        return plan;
    }
}
