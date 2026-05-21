package rs.skola.platforma.predmeti.web;

import jakarta.validation.constraints.*;

public record KreirajPredmetRequest(
        @NotBlank @Size(max = 255) String naziv,
        @Min(1) @Max(4) Short razred,
        @Min(1) @Max(20) Short fondCasova
) {}
