package rs.skola.platforma.raspored;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.raspored.web.NemapiraniProfesorResponse;
import rs.skola.platforma.raspored.web.RasporedStavkaResponse;
import rs.skola.platforma.raspored.web.UvozRasporedaResponse;
import rs.skola.platforma.raspored.web.VerzijaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Raspored", description = "Uvoz XML rasporeda, prikaz mog rasporeda i rasporeda dana")
@RestController
@RequestMapping("/api/v1/raspored")
@RequiredArgsConstructor
public class RasporedController {

    private final RasporedService service;

    @PostMapping(value = "/uvoz", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Uvoz XML rasporeda (SpreadsheetML 2003)")
    public ApiResponse<UvozRasporedaResponse> uvezi(
            @RequestParam("fajl") MultipartFile fajl,
            @RequestParam("skolskaGodina")
            @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}", message = "Format: 2024/2025") String skolskaGodina,
            @RequestParam(value = "naziv", required = false) String naziv,
            @RequestParam(value = "aktivan", required = false, defaultValue = "true") boolean aktivan
    ) {
        return ApiResponse.ok(service.uvezi(fajl, skolskaGodina, naziv, aktivan));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sopstveni raspored (aktivna verzija)")
    public ApiResponse<List<RasporedStavkaResponse>> mojRaspored(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.mojRaspored(ja));
    }

    @GetMapping("/verzije")
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR','ADMIN')")
    @Operation(summary = "Sve verzije rasporeda za skolu (najnovije prvo)")
    public ApiResponse<List<VerzijaResponse>> verzije() {
        return ApiResponse.ok(service.sveVerzije());
    }

    @PostMapping("/verzije/{id}/aktiviraj")
    @PreAuthorize("hasAnyRole('KOORDINATOR','ADMIN')")
    @Operation(summary = "Aktivira datu verziju i deaktivira ostale")
    public ApiResponse<VerzijaResponse> aktivirajVerziju(@PathVariable UUID id) {
        return ApiResponse.ok(service.aktivirajVerziju(id));
    }

    @DeleteMapping("/verzije/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise verziju rasporeda (ON DELETE CASCADE brise sve stavke)")
    public void obrisiVerziju(@PathVariable UUID id) {
        service.obrisiVerziju(id);
    }

    @GetMapping("/nemapirani-profesori")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Lista profesora iz rasporeda koji nisu mapirani na korisnicki nalog")
    public ApiResponse<List<NemapiraniProfesorResponse>> nemapirani() {
        return ApiResponse.ok(service.nemapiraniProfesori());
    }

    @PostMapping("/mapiraj-profesora")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Manuelno mapira label iz rasporeda na postojeceg korisnika")
    public ApiResponse<MapiranjeOdgovor> mapiraj(@Valid @RequestBody MapiranjeZahtev req) {
        int azurirano = service.mapirajProfesora(req.nastavnikLabel(), req.korisnikId());
        return ApiResponse.ok(new MapiranjeOdgovor(azurirano));
    }

    public record MapiranjeZahtev(@NotBlank String nastavnikLabel, @NotNull UUID korisnikId) {}

    public record MapiranjeOdgovor(int brojAzuriranihStavki) {}
}
