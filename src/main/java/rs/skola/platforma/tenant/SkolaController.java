package rs.skola.platforma.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;
import rs.skola.platforma.tenant.web.SkolaMapper;
import rs.skola.platforma.tenant.web.SkolaResponse;

import java.util.UUID;

/**
 * Endpointi koje koristi KOORDINATOR za podesavanje sopstvene skole.
 * Mail za PP/planove se NE postavlja kroz SUPER_ADMIN endpoint nego ovde —
 * svaka skola je odgovorna za svoju komunikaciju.
 */
@Tag(name = "Moja skola", description = "Podesavanja skole koja menja koordinator")
@RestController
@RequestMapping("/api/v1/skola")
@RequiredArgsConstructor
public class SkolaController {

    private final SkolaRepository skolaRepository;
    private final SkolaMapper mapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Informacije o skoli trenutnog korisnika")
    public ApiResponse<SkolaResponse> moja() {
        UUID skolaId = TenantContext.require();
        Skola s = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        return ApiResponse.ok(mapper.toResponse(s));
    }

    @PatchMapping("/mail-planovi")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Postavlja mail za primanje generisanih planova")
    public ApiResponse<SkolaResponse> postaviMailPlanovi(@Valid @RequestBody PostaviMailRequest req) {
        UUID skolaId = TenantContext.require();
        Skola s = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        // null/blank brise postojecu adresu
        String mail = req.mailPlanovi();
        s.setMailPlanovi(mail == null || mail.isBlank() ? null : mail.trim());
        return ApiResponse.ok(mapper.toResponse(s));
    }

    public record PostaviMailRequest(
            @Email @Size(max = 255) String mailPlanovi
    ) {}
}
