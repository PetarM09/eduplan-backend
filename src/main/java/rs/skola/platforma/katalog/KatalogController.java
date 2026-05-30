package rs.skola.platforma.katalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.katalog.web.IshodResponse;
import rs.skola.platforma.katalog.web.NastavnaJedinicaResponse;
import rs.skola.platforma.katalog.web.PadajuciMeniResponse;
import rs.skola.platforma.katalog.web.TemaResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Katalog", description = "Teme, nastavne jedinice, ishodi i padajuci meniji za planove")
@RestController
@RequestMapping("/api/v1/katalog")
@RequiredArgsConstructor
public class KatalogController {

    private final KatalogService service;

    // -------- TEME --------

    @GetMapping("/teme")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sve teme predmeta")
    public ApiResponse<List<TemaResponse>> temePredmeta(@RequestParam UUID predmetId) {
        return ApiResponse.ok(service.temePredmeta(predmetId));
    }

    @GetMapping("/teme/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete pretraga tema predmeta")
    public ApiResponse<List<TemaResponse>> pretragaTema(@RequestParam UUID predmetId,
                                                         @RequestParam(required = false) String q) {
        return ApiResponse.ok(service.pretragaTema(predmetId, q));
    }

    // -------- NASTAVNE JEDINICE --------

    @GetMapping("/jedinice")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sve nastavne jedinice teme")
    public ApiResponse<List<NastavnaJedinicaResponse>> jediniceTeme(@RequestParam UUID temaId) {
        return ApiResponse.ok(service.jediniceTeme(temaId));
    }

    @GetMapping("/jedinice/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete pretraga nastavnih jedinica")
    public ApiResponse<List<NastavnaJedinicaResponse>> pretragaJedinica(@RequestParam UUID temaId,
                                                                         @RequestParam(required = false) String q) {
        return ApiResponse.ok(service.pretragaJedinica(temaId, q));
    }

    // -------- ISHODI --------

    @GetMapping("/ishodi")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ishodi vezani za temu")
    public ApiResponse<List<IshodResponse>> ishodiTeme(@RequestParam UUID temaId) {
        return ApiResponse.ok(service.ishodiTeme(temaId));
    }

    @PostMapping("/ishodi")
    @PreAuthorize("hasRole('NASTAVNIK')")
    @Operation(summary = "Dodaje novi ishod za temu (auto-save u katalog)")
    public ApiResponse<IshodResponse> kreirajIshod(@RequestBody KreirajIshodRequest req) {
        var i = service.kreirajIshod(req.temaId(), req.opis());
        return ApiResponse.ok(new IshodResponse(i.getId(), req.temaId(), i.getOpis()));
    }

    @DeleteMapping("/teme/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise temu sa svim nastavnim jedinicama i ishodima (cascade)")
    public void obrisiTemu(@PathVariable UUID id) {
        service.obrisiTemu(id);
    }

    @DeleteMapping("/jedinice/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise nastavnu jedinicu — tema i ishodi te teme ostaju")
    public void obrisiJedinicu(@PathVariable UUID id) {
        service.obrisiJedinicu(id);
    }

    @DeleteMapping("/ishodi/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise ishod")
    public void obrisiIshod(@PathVariable UUID id) {
        service.obrisiIshod(id);
    }

    // -------- PADAJUCI MENIJI --------

    @GetMapping("/tipovi-casa")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tipovi casa (sistemski + skolski)")
    public ApiResponse<List<PadajuciMeniResponse>> tipoviCasa() {
        return ApiResponse.ok(service.tipoviCasa());
    }

    @GetMapping("/metode-rada")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Metode rada (sistemske + skolske)")
    public ApiResponse<List<PadajuciMeniResponse>> metodeRada() {
        return ApiResponse.ok(service.metodeRada());
    }

    public record KreirajIshodRequest(
            @NotNull UUID temaId,
            @NotBlank @Size(max = 2000) String opis
    ) {}
}
