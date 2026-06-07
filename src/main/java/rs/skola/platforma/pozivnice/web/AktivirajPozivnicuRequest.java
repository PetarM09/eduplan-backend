package rs.skola.platforma.pozivnice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AktivirajPozivnicuRequest(
        @NotBlank @Size(min = 8, max = 100) String lozinka
) {}
