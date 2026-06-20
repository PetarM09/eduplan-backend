package rs.skola.platforma.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromenaLozinkeRequest(
        @NotBlank @Size(max = 200) String staraLozinka,
        @NotBlank @Size(min = 8, max = 100) String novaLozinka
) {}
