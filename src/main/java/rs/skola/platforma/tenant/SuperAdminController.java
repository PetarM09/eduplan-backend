package rs.skola.platforma.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.web.KorisnikResponse;
import rs.skola.platforma.korisnici.web.KreirajKorisnikaRequest;
import rs.skola.platforma.tenant.web.KreirajSkoluRequest;
import rs.skola.platforma.tenant.web.SkolaResponse;

import java.time.LocalDate;
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

    // -------- Lifecycle skole --------

    @PostMapping("/skole/{id}/aktiviraj")
    @Operation(summary = "Reaktivira skolu (login svih korisnika ponovo dozvoljen)")
    public ApiResponse<SkolaResponse> aktivirajSkolu(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.aktivirajSkolu(id));
    }

    @PostMapping("/skole/{id}/deaktiviraj")
    @Operation(summary = "Deaktivira skolu (svi korisnici dobijaju 401 pri login-u)")
    public ApiResponse<SkolaResponse> deaktivirajSkolu(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.deaktivirajSkolu(id));
    }

    @PatchMapping("/skole/{id}/vazi-do")
    @Operation(summary = "Postavlja ili uklanja datum automatske deaktivacije (null = bez ogranicenja)")
    public ApiResponse<SkolaResponse> postaviVaziDo(@PathVariable UUID id,
                                                     @RequestBody PostaviVaziDoRequest req) {
        return ApiResponse.ok(tenantService.postaviVaziDo(id, req.vaziDo()));
    }

    public record PostaviVaziDoRequest(LocalDate vaziDo) {}

    // -------- Korisnici po skoli --------

    @GetMapping("/skole/{id}/korisnici")
    @Operation(summary = "Svi korisnici jedne skole (po prezimenu)")
    public ApiResponse<List<KorisnikResponse>> korisniciSkole(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.korisniciSkole(id));
    }

    @PostMapping("/korisnici/{id}/aktiviraj")
    @Operation(summary = "Reaktivira korisnika (login se ponovo dozvoljava)")
    public ApiResponse<KorisnikResponse> aktiviraj(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.aktivirajKorisnika(id));
    }

    @PostMapping("/korisnici/{id}/deaktiviraj")
    @Operation(summary = "Deaktivira korisnika (login se vise ne dozvoljava)")
    public ApiResponse<KorisnikResponse> deaktiviraj(@PathVariable UUID id) {
        return ApiResponse.ok(tenantService.deaktivirajKorisnika(id));
    }

    @PatchMapping("/korisnici/{id}/uloga")
    @Operation(summary = "Menja ulogu korisnika u okviru iste skole")
    public ApiResponse<KorisnikResponse> promeniUlogu(@PathVariable UUID id,
                                                       @Valid @RequestBody PromeniUloguRequest req) {
        return ApiResponse.ok(tenantService.promeniUlogu(id, req.uloga()));
    }

    @DeleteMapping("/korisnici/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise korisnika ako nema vezanih zapisa (planovi, izvestaji)")
    public void obrisi(@PathVariable UUID id) {
        tenantService.obrisiKorisnika(id);
    }

    public record PromeniUloguRequest(@NotNull Uloga uloga) {}
}
