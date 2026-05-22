package rs.skola.platforma.odeljenja.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record KreirajOdeljenjeRequest(
        @NotNull Short razred,
        @NotBlank @Size(min = 1, max = 5) String oznaka,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}", message = "Format: 2024/2025") String skolskaGodina,
        UUID staresinaId
) {}
