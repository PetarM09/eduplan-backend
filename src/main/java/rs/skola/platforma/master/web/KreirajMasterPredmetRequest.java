package rs.skola.platforma.master.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KreirajMasterPredmetRequest(
        @NotNull @Min(1) @Max(12) Short razred,
        @NotBlank String naziv,
        @NotNull @Min(0) Short fondTeorija,
        @NotNull @Min(0) Short fondVezbe,
        @NotNull @Min(0) Short fondBlok,
        Boolean obavezan,
        Short redosled
) {}
