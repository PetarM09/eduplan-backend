package rs.skola.platforma.planovi.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record KreirajGodisnjiPlanRequest(
        @NotNull UUID predmetId,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina,
        @Min(1) @Max(4) Short razred,
        List<UUID> odeljenjaIds,

        @Size(max = 5000) String ciljeviZadaci,
        @Size(max = 500) String udzebenik,
        @Size(max = 500) String autori,
        @Size(max = 5000) String literatura,
        @Min(0) @Max(500) Short godisnjiFond,
        @Min(0) @Max(20) Short nedeljniFond,
        @Size(max = 5000) String dopunskiRad,
        @Size(max = 5000) String dodatniRad,
        @Size(max = 5000) String napomene,

        @Valid @NotEmpty(message = "Plan mora imati bar jednu temu") List<StavkaTemeRequest> teme
) {

    public record StavkaTemeRequest(
            UUID temaId,                                  // ako postoji u katalogu
            @Size(max = 500) String nazivTeme,            // alternativno: novi naziv -> auto-save
            @Min(0) Short redniBroj,
            @Min(0) @Max(500) Short casObrada,
            @Min(0) @Max(500) Short casUtvrd,
            @Min(0) @Max(500) Short casOstalo,
            @Min(0) @Max(500) Short ukupnoCasova,
            List<String> meseci,                          // ["IX","X","XI",...] meseci u kojima se predaje
            List<UUID> ishodiIds,                         // postojeci ishodi iz kataloga
            List<String> noviIshodi                       // free-text -> auto-save kao novi ishodi
    ) {}
}
