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
import rs.skola.platforma.rotacija.web.AzurirajNedeljuRequest;
import rs.skola.platforma.rotacija.web.KreirajRotacijuRequest;
import rs.skola.platforma.rotacija.web.RotNedeljaResponse;
import rs.skola.platforma.rotacija.web.RotacijaResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rotacija", description = "Rotacioni raspored za grupne casove (vezbe)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RotacijaController {

    private final RotacijaService service;

    @PostMapping("/rotacija")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Kreira novu rotacionu konfiguraciju za predmet/odeljenja")
    public ApiResponse<RotacijaResponse> kreiraj(@AuthenticationPrincipal CustomUserDetails ja,
                                                  @Valid @RequestBody KreirajRotacijuRequest req) {
        return ApiResponse.ok(service.kreirajKonfiguraciju(ja, req));
    }

    @PostMapping("/rotacija/{id}/generisi")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Generise pun ciklus C(N,K) — brise prethodne nedelje ako postoje")
    public ApiResponse<RotacijaResponse> generisi(@PathVariable UUID id) {
        return ApiResponse.ok(service.generisi(id));
    }

    @GetMapping("/rotacija")
    @PreAuthorize("hasAnyRole('NASTAVNIK','ADMIN','DIREKTOR','KOORDINATOR','PP_SLUZBA')")
    @Operation(summary = "Sve rotacione konfiguracije skole")
    public ApiResponse<List<RotacijaResponse>> sve() {
        return ApiResponse.ok(service.sveZaSkolu());
    }

    @GetMapping("/rotacija/moje")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Moje rotacione konfiguracije")
    public ApiResponse<List<RotacijaResponse>> moje(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.mojeKonfiguracije(ja));
    }

    @GetMapping("/rotacija/{id}")
    @PreAuthorize("hasAnyRole('NASTAVNIK','ADMIN','DIREKTOR','KOORDINATOR','PP_SLUZBA')")
    @Operation(summary = "Detaljan pregled rotacije sa svim nedeljama i statistikom balansa")
    public ApiResponse<RotacijaResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @PutMapping("/rotacija/nedelje/{id}")
    @PreAuthorize("hasAnyRole('NASTAVNIK','ADMIN','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "Rucno premestanje odeljenja u datoj nedelji")
    public ApiResponse<RotNedeljaResponse> azurirajNedelju(@PathVariable UUID id,
                                                            @Valid @RequestBody AzurirajNedeljuRequest req) {
        return ApiResponse.ok(service.azurirajNedelju(id, req));
    }

    @DeleteMapping("/rotacija/{id}")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise rotacionu konfiguraciju (i sve nedelje)")
    public void obrisi(@PathVariable UUID id) {
        service.obrisi(id);
    }
}
