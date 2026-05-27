package rs.skola.platforma.odeljenja;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.odeljenja.web.KreirajOdeljenjeRequest;
import rs.skola.platforma.odeljenja.web.OdeljenjeResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Odeljenja", description = "Upravljanje odeljenjima i staresinama")
@RestController
@RequestMapping("/api/v1/odeljenja")
@RequiredArgsConstructor
public class OdeljenjeController {

    private final OdeljenjeService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sva odeljenja u skoli")
    public ApiResponse<List<OdeljenjeResponse>> svaOdeljenja() {
        return ApiResponse.ok(service.svaOdeljenjaSkole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @Operation(summary = "Kreira novo odeljenje")
    public ApiResponse<OdeljenjeResponse> kreiraj(@Valid @RequestBody KreirajOdeljenjeRequest req) {
        return ApiResponse.ok(service.kreiraj(req));
    }

    @PutMapping("/{id}/staresina")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @Operation(summary = "Dodeli razrednog staresinu odeljenju")
    public ApiResponse<OdeljenjeResponse> postaviStaresinu(@PathVariable UUID id, @RequestParam(required = false) UUID staresinaId) {
        return ApiResponse.ok(service.postaviStaresinu(id, staresinaId));
    }

    @PostMapping("/{id}/deaktiviraj")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Deaktivira odeljenje (preporuceno ako ima vezane podatke)")
    public ApiResponse<Void> deaktiviraj(@PathVariable UUID id) {
        service.deaktiviraj(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise odeljenje ako nema vezane podatke (planovi, izvestaji, raspored)")
    public void obrisi(@PathVariable UUID id) {
        service.obrisi(id);
    }
}
