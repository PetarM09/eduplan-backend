package rs.skola.platforma.master.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KreirajTipSkoleRequest(
        @NotBlank String kod,
        @NotBlank String naziv,
        @NotNull @Min(1) @Max(12) Short ukupnoRazreda
) {}
