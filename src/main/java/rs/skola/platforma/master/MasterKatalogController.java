package rs.skola.platforma.master;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.master.web.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Master katalog", description = "Globalni katalog tipova skole, obrazovnih profila i predmeta")
@RestController
@RequestMapping("/api/v1/master")
@RequiredArgsConstructor
public class MasterKatalogController {

    private final MasterKatalogService service;

    // -------- Tipovi skole --------

    @GetMapping("/tipovi-skole")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista svih tipova skole")
    public ApiResponse<List<TipSkoleResponse>> sviTipovi() {
        return ApiResponse.ok(service.sviTipovi());
    }

    @PostMapping("/tipovi-skole")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodaje tip skole (samo SUPER_ADMIN)")
    public ApiResponse<TipSkoleResponse> kreirajTip(@Valid @RequestBody KreirajTipSkoleRequest req) {
        return ApiResponse.ok(service.kreirajTip(req));
    }

    @DeleteMapping("/tipovi-skole/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise tip skole (samo ako nema profila)")
    public void obrisiTip(@PathVariable UUID id) {
        service.obrisiTip(id);
    }

    // -------- Obrazovni profili --------

    @GetMapping("/profili")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista profila (opciono filtrirano po tipu)")
    public ApiResponse<List<ObrazovniProfilResponse>> sviProfili(
            @RequestParam(required = false) UUID tipSkoleId) {
        return ApiResponse.ok(service.sviProfili(tipSkoleId));
    }

    @PostMapping("/profili")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Kreira obrazovni profil (samo SUPER_ADMIN)")
    public ApiResponse<ObrazovniProfilResponse> kreirajProfil(@Valid @RequestBody KreirajObrazovniProfilRequest req) {
        return ApiResponse.ok(service.kreirajProfil(req));
    }

    @DeleteMapping("/profili/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise profil i sve njegove predmete (CASCADE)")
    public void obrisiProfil(@PathVariable UUID id) {
        service.obrisiProfil(id);
    }

    // -------- Predmeti profila --------

    @GetMapping("/profili/{id}/predmeti")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista predmeta jednog profila (sortirano po razredu)")
    public ApiResponse<List<MasterPredmetResponse>> predmetiProfila(@PathVariable UUID id) {
        return ApiResponse.ok(service.predmetiProfila(id));
    }

    @PostMapping("/profili/{id}/predmeti")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodaje predmet u profil (T+V+B fond)")
    public ApiResponse<MasterPredmetResponse> kreirajPredmet(@PathVariable UUID id,
                                                              @Valid @RequestBody KreirajMasterPredmetRequest req) {
        return ApiResponse.ok(service.kreirajPredmet(id, req));
    }

    @PutMapping("/predmeti/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Azurira predmet")
    public ApiResponse<MasterPredmetResponse> azurirajPredmet(@PathVariable UUID id,
                                                                @Valid @RequestBody KreirajMasterPredmetRequest req) {
        return ApiResponse.ok(service.azurirajPredmet(id, req));
    }

    @DeleteMapping("/predmeti/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise predmet")
    public void obrisiPredmet(@PathVariable UUID id) {
        service.obrisiPredmet(id);
    }
}
