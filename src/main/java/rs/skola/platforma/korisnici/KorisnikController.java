package rs.skola.platforma.korisnici;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.web.KorisnikResponse;
import rs.skola.platforma.korisnici.web.KreirajKorisnikaRequest;

import java.util.List;
import java.util.UUID;

@Tag(name = "Korisnici", description = "Korisnici u okviru skole (KOORDINATOR i DIREKTOR)")
@RestController
@RequestMapping("/api/v1/korisnici")
@RequiredArgsConstructor
public class KorisnikController {

    private final KorisnikService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR')")
    @Operation(summary = "Lista svih korisnika u skoli (po prezimenu)")
    public ApiResponse<List<KorisnikResponse>> svi() {
        return ApiResponse.ok(service.sviKorisniciSkole());
    }

    @GetMapping("/po-ulozi/{uloga}")
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR','ADMIN')")
    @Operation(summary = "Lista korisnika sa datom ulogom u skoli")
    public ApiResponse<List<KorisnikResponse>> poUlozi(@PathVariable Uloga uloga) {
        return ApiResponse.ok(service.sviPoUlozi(uloga));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KOORDINATOR','DIREKTOR')")
    @Operation(summary = "Pregled jednog korisnika u skoli")
    public ApiResponse<KorisnikResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(service.pregled(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Kreira novog korisnika u skoli (DIREKTOR/ADMIN/PP_SLUZBA/NASTAVNIK)")
    public ApiResponse<KorisnikResponse> kreiraj(@Valid @RequestBody KreirajKorisnikaRequest req) {
        return ApiResponse.ok(service.kreirajKorisnika(req));
    }

    @PostMapping("/{id}/deaktiviraj")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @Operation(summary = "Deaktivira korisnika (login se vise ne dozvoljava)")
    public ApiResponse<KorisnikResponse> deaktiviraj(@PathVariable UUID id) {
        return ApiResponse.ok(service.deaktiviraj(id));
    }
}
