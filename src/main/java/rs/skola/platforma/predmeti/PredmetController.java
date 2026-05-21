package rs.skola.platforma.predmeti;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.predmeti.web.DodeliOdeljenjaRequest;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Predmeti", description = "Registar predmeta skole i dodeljivanje odeljenima")
@RestController
@RequestMapping("/api/v1/predmeti")
@RequiredArgsConstructor
public class PredmetController {

    private final PredmetService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aktivni predmeti skole")
    public ApiResponse<List<PredmetResponse>> sviAktivni() {
        return ApiResponse.ok(service.sviAktivni());
    }

    @GetMapping("/svi")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN','DIREKTOR')")
    @Operation(summary = "Svi predmeti ukljucujuci deaktivirane")
    public ApiResponse<List<PredmetResponse>> svi() {
        return ApiResponse.ok(service.svi());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PredmetResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @Operation(summary = "Kreira novi predmet u skoli")
    public ApiResponse<PredmetResponse> kreiraj(@Valid @RequestBody KreirajPredmetRequest req) {
        return ApiResponse.ok(service.kreiraj(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    public ApiResponse<PredmetResponse> azuriraj(@PathVariable UUID id, @Valid @RequestBody KreirajPredmetRequest req) {
        return ApiResponse.ok(service.azuriraj(id, req));
    }

    @PutMapping("/{id}/odeljenja")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @Operation(summary = "Postavlja listu odeljenja u kojima se predmet realizuje")
    public ApiResponse<PredmetResponse> dodeliOdeljenja(@PathVariable UUID id,
                                                        @Valid @RequestBody DodeliOdeljenjaRequest req) {
        return ApiResponse.ok(service.dodeliOdeljenja(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deaktivira predmet (ne brise se zbog FK-ova ka planovima)")
    public void deaktiviraj(@PathVariable UUID id) {
        service.deaktiviraj(id);
    }
}
