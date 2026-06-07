package rs.skola.platforma.onboarding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.onboarding.web.PokreniWizardRequest;
import rs.skola.platforma.onboarding.web.WizardPregledRequest;
import rs.skola.platforma.onboarding.web.WizardPregledResponse;
import rs.skola.platforma.onboarding.web.WizardRezultatResponse;

@Tag(name = "Skolski onboarding wizard", description = "Kreira predmete i odeljenja iz master kataloga")
@RestController
@RequestMapping("/api/v1/skola-onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService service;

    @PostMapping("/pregled")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Pregled sta ce wizard kreirati (bez upisivanja u bazu)")
    public ApiResponse<WizardPregledResponse> pregled(@Valid @RequestBody WizardPregledRequest req) {
        return ApiResponse.ok(service.pregled(req));
    }

    @PostMapping("/pokreni")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Pokrece wizard — kreira predmete i odeljenja u skoli")
    public ApiResponse<WizardRezultatResponse> pokreni(@Valid @RequestBody PokreniWizardRequest req) {
        return ApiResponse.ok(service.pokreni(req));
    }
}
