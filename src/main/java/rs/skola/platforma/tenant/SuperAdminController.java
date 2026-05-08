package rs.skola.platforma.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.korisnici.web.KorisnikResponse;
import rs.skola.platforma.korisnici.web.KreirajKorisnikaRequest;
import rs.skola.platforma.tenant.web.KreirajSkoluRequest;
import rs.skola.platforma.tenant.web.SkolaResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Super admin", description = "Globalne operacije: skole i koordinatori (samo SUPER_ADMIN)")
@RestController
@RequestMapping("/api/v1/super")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final TenantService tenantService;

    @GetMapping("/skole")
    @Operation(summary = "Lista svih skola u sistemu")
    public ApiResponse<List<SkolaResponse>> sveSkole() {
        return ApiResponse.ok(tenantService.sveSkole());
    }

    @PostMapping("/skole")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Kreira novu skolu (tenant)")
    public ApiResponse<SkolaResponse> kreirajSkolu(@Valid @RequestBody KreirajSkoluRequest req) {
        return ApiResponse.ok(tenantService.kreirajSkolu(req));
    }

    @PostMapping("/skole/{id}/koordinator")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Kreira KOORDINATOR nalog za skolu (administrator skole)")
    public ApiResponse<KorisnikResponse> kreirajKoordinatora(@PathVariable UUID id,
                                                              @Valid @RequestBody KreirajKorisnikaRequest req) {
        return ApiResponse.ok(tenantService.kreirajKoordinatora(id, req));
    }
}
