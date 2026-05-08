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
import rs.skola.platforma.raspored.web.RasporedStavkaResponse;
import rs.skola.platforma.raspored.web.UvozRasporedaResponse;

import java.util.List;

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
}
