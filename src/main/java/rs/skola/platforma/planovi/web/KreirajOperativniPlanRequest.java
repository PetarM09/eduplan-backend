package rs.skola.platforma.planovi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record KreirajOperativniPlanRequest(
        @NotNull UUID predmetId,
        @NotNull UUID odeljenjeId,
        @NotNull @Min(1) @Max(12) Short mesec,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina,
        @Min(1) @Max(20) Short nedeljniFond,
        @Size(max = 5000) String samoprocenaIshoda,
        @Size(max = 5000) String napomene,
        @Valid @NotEmpty(message = "Plan mora imati bar jednu stavku (cas)") List<StavkaCasaRequest> stavke
) {

    public record StavkaCasaRequest(
            @NotNull @Min(1) @Max(99) Short redniBrojCasa,

            UUID temaId,                             // postojeca tema iz kataloga
            @Size(max = 500) String nazivTeme,       // ili nov naziv → findOrCreate

            UUID nastavnaJedinicaId,                 // postojeca jedinica
            @Size(max = 500) String nazivJedinice,   // ili nov naziv → findOrCreate

            @NotNull UUID tipCasaId,
            UUID metodaRadaId,

            List<UUID> ishodiIds,                    // postojeci ishodi
            List<String> noviIshodi,                 // free-text → auto-save

            @Valid List<MedjupredmetnoRequest> medjupredmetno,

            @Size(max = 2000) String evaluacija
    ) {}

    public record MedjupredmetnoRequest(
            @NotNull UUID predmetId,
            @NotBlank @Size(max = 2000) String opisKompetencije
    ) {}
}
