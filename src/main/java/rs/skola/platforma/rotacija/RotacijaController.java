package rs.skola.platforma.rotacija;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.rotacija.web.DetekcijaVezbiResponse;
import rs.skola.platforma.rotacija.web.KreirajRotacijuRequest;
import rs.skola.platforma.rotacija.web.RotacijaResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rotacija", description = "Rotacioni raspored grupa za vezbe jednog odeljenja")
@RestController
@RequestMapping("/api/v1/rotacija")
@RequiredArgsConstructor
public class RotacijaController {

    private final RotacijaService service;

    @GetMapping("/vezbe/{odeljenjeId}")
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR','ADMIN')")
    @Operation(summary = "Detekcija termina vezbi (2+ profesora istovremeno) za izabrano odeljenje")
    public ApiResponse<DetekcijaVezbiResponse> detektuj(@PathVariable UUID odeljenjeId) {
        return ApiResponse.ok(service.detektujVezbe(odeljenjeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Kreira rotaciju za odeljenje i automatski generise dodele grupa")
    public ApiResponse<RotacijaResponse> kreiraj(@AuthenticationPrincipal CustomUserDetails ja,
                                                  @Valid @RequestBody KreirajRotacijuRequest req) {
        return ApiResponse.ok(service.kreiraj(ja, req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR','ADMIN','PP_SLUZBA')")
    @Operation(summary = "Sve rotacije skole")
    public ApiResponse<List<RotacijaResponse>> sve() {
        return ApiResponse.ok(service.sveZaSkolu());
    }

    @GetMapping("/moje")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Rotacije u kojima trenutni nastavnik predaje vezbe")
    public ApiResponse<List<RotacijaResponse>> moje(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.moje(ja));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detaljan pregled rotacije sa svim nedeljama i dodelama")
    public ApiResponse<RotacijaResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Brise rotaciju zajedno sa svim dodelama")
    public void obrisi(@PathVariable UUID id) {
        service.obrisi(id);
    }
}
