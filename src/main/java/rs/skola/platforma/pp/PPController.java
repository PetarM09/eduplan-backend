package rs.skola.platforma.pp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.web.ApiResponse;
import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.domain.PPStatus;
import rs.skola.platforma.pp.web.PPDashboardResponse;
import rs.skola.platforma.pp.web.PPIzvestajRequest;
import rs.skola.platforma.pp.web.PPIzvestajResponse;
import rs.skola.platforma.pp.web.StatistikaResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "PP sluzba", description = "PP izvestaji starešina + dashboard i statistika za PP sluzbu")
@RestController
@RequestMapping("/api/v1/pp")
@RequiredArgsConstructor
public class PPController {

    private final PPService ppService;
    private final PPDashboardService dashboardService;
    private final StatistikaAggregatorService statistikaService;
    private final PPEksportService eksportService;

    // -------- IZVESTAJI (starešina) --------

    @PostMapping("/izvestaj")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    @Operation(summary = "Kreiraj ili azuriraj PP izvestaj za odeljenje (staresina)")
    public ApiResponse<PPIzvestajResponse> kreirajIzvestaj(@AuthenticationPrincipal CustomUserDetails ja,
            @Valid @RequestBody PPIzvestajRequest req) {
        return ApiResponse.ok(ppService.kreirajIliAzuriraj(ja, req));
    }

    @PostMapping("/izvestaj/{id}/podnesi")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    public ApiResponse<PPIzvestajResponse> podnesi(@PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails ja) {
        return ApiResponse.ok(ppService.podnesi(id, ja));
    }

    @PostMapping("/izvestaj/{id}/prihvati")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR')")
    public ApiResponse<PPIzvestajResponse> prihvati(@PathVariable UUID id) {
        return ApiResponse.ok(ppService.prihvati(id));
    }

    @PostMapping("/izvestaj/{id}/vrati-na-doradu")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR')")
    public ApiResponse<PPIzvestajResponse> vratiNaDoradu(@PathVariable UUID id) {
        return ApiResponse.ok(ppService.vratiNaDoradu(id));
    }

    @GetMapping("/izvestaji/moji")
    @PreAuthorize("hasAnyRole('NASTAVNIK','KOORDINATOR')")
    public ApiResponse<List<PPIzvestajResponse>> mojiIzvestaji(@AuthenticationPrincipal CustomUserDetails ja,
            @RequestParam(required = false) String skolskaGodina) {
        return ApiResponse.ok(ppService.mojiIzvestaji(ja, skolskaGodina));
    }

    @GetMapping("/izvestaji/svi")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR','KOORDINATOR')")
    public ApiResponse<List<PPIzvestajResponse>> sviIzvestaji(
            @RequestParam(required = false) String skolskaGodina,
            @RequestParam(required = false) PPPeriod period,
            @RequestParam(required = false) PPStatus status) {
        return ApiResponse.ok(ppService.sviZaSkolu(skolskaGodina, period, status));
    }

    @GetMapping("/izvestaj/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PPIzvestajResponse> pregled(@PathVariable UUID id) {
        return ApiResponse.ok(ppService.pregled(id));
    }

    @DeleteMapping("/izvestaj/{id}")
    @PreAuthorize("hasRole('KOORDINATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Brise PP izvestaj (koordinator skole)")
    public void obrisi(@PathVariable UUID id) {
        ppService.obrisi(id);
    }

    // -------- DASHBOARD I STATISTIKA --------

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR','KOORDINATOR')")
    @Operation(summary = "PP dashboard: planovi + izvestaji + statusi za skolsku godinu")
    public ApiResponse<PPDashboardResponse> dashboard(@RequestParam(required = false) String skolskaGodina) {
        return ApiResponse.ok(dashboardService.dashboard(skolskaGodina));
    }

    @GetMapping("/statistika")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR')")
    @Operation(summary = "Sumirana statistika izvestaja za skolsku godinu i period")
    public ApiResponse<StatistikaResponse> statistika(@RequestParam String skolskaGodina,
            @RequestParam PPPeriod period) {
        return ApiResponse.ok(statistikaService.agregiraj(skolskaGodina, period));
    }

    @GetMapping("/eksport/excel")
    @PreAuthorize("hasAnyRole('PP_SLUZBA','DIREKTOR')")
    @Operation(summary = "Excel eksport planova, izvestaja i statistike")
    public ResponseEntity<byte[]> eksportExcel(@RequestParam String skolskaGodina) {
        byte[] bytes = eksportService.generisiExcel(skolskaGodina);
        String fileName = "pp-izvestaj-%s.xlsx".formatted(skolskaGodina.replace("/", "-"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
