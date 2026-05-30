package rs.skola.platforma.zamene;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.zamene.web.DodeliZamenikaRequest;
import rs.skola.platforma.zamene.web.KandidatZamenikResponse;
import rs.skola.platforma.zamene.web.OdbijZamenuRequest;
import rs.skola.platforma.zamene.web.PrijaviOdsustvoRequest;
import rs.skola.platforma.zamene.web.ZamenaResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Zamene", description = "Prijava odsustva, predlozi zamenika i workflow odobravanja")
@RestController
@RequestMapping("/api/v1/zamene")
@RequiredArgsConstructor
public class ZameneController {

    private final ZameneService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Prijava odsustva — kreira PREDLOZENA zamene za navedene casove")
    public ApiResponse<List<ZamenaResponse>> prijaviOdsustvo(@AuthenticationPrincipal CustomUserDetails ja,
                                                              @Valid @RequestBody PrijaviOdsustvoRequest req) {
        return ApiResponse.ok(service.prijaviOdsustvo(ja, req));
    }

    @GetMapping("/danas")
    @PreAuthorize("hasAnyRole('ADMIN','DIREKTOR','KOORDINATOR','PP_SLUZBA')")
    @Operation(summary = "Sve zamene za dati datum (default danas)")
    public ApiResponse<List<ZamenaResponse>> zameneDana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        return ApiResponse.ok(service.zameneDana(datum == null ? LocalDate.now() : datum));
    }

    @GetMapping("/moje/odsutni")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Moje zamene gde sam ja prijavio odsustvo")
    public ApiResponse<List<ZamenaResponse>> mojeKaoOdsutni(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.mojeKaoOdsutni(ja));
    }

    @GetMapping("/moje/zamenik")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Moje zamene gde sam ja zamenik")
    public ApiResponse<List<ZamenaResponse>> mojeKaoZamenik(@AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.mojeKaoZamenik(ja));
    }

    @GetMapping("/{id}/kandidati")
    @PreAuthorize("hasAnyRole('ADMIN','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "Predlog kandidata za zamenika (slobodni u tom casu, sortirano po opterecenju)")
    public ApiResponse<List<KandidatZamenikResponse>> kandidati(@PathVariable UUID id) {
        return ApiResponse.ok(service.predloziKandidate(id));
    }

    @PutMapping("/{id}/zamenik")
    @PreAuthorize("hasAnyRole('ADMIN','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "Dodeli zamenika izabranog iz kandidata")
    public ApiResponse<ZamenaResponse> dodeliZamenika(@PathVariable UUID id,
                                                       @Valid @RequestBody DodeliZamenikaRequest req) {
        return ApiResponse.ok(service.dodeliZamenika(id, req));
    }

    @PutMapping("/{id}/odobri")
    @PreAuthorize("hasAnyRole('DIREKTOR','ADMIN')")
    @Operation(summary = "Odobravanje zamene (PREDLOZENA -> ODOBRENA)")
    public ApiResponse<ZamenaResponse> odobri(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.odobri(id, ja));
    }

    @PutMapping("/{id}/odbij")
    @PreAuthorize("hasAnyRole('DIREKTOR','ADMIN')")
    @Operation(summary = "Odbijanje zamene sa razlogom")
    public ApiResponse<ZamenaResponse> odbij(@PathVariable UUID id,
                                              @Valid @RequestBody OdbijZamenuRequest req,
                                              @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.odbij(id, req, ja));
    }

    @PutMapping("/{id}/otkazi")
    @PreAuthorize("hasAnyRole('NASTAVNIK','ADMIN','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "Otkazivanje zamene (odsutni ili admin)")
    public ApiResponse<ZamenaResponse> otkazi(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(service.otkazi(id, ja));
    }
}
