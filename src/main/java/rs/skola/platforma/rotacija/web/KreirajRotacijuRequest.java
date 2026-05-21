package rs.skola.platforma.rotacija.web;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record KreirajRotacijuRequest(
        @NotBlank @Size(max = 255) String naziv,
        UUID predmetId,
        @NotEmpty @Size(min = 2, max = 12, message = "Mora biti izmedju 2 i 12 odeljenja") List<UUID> odeljenjaIds,
        @NotNull @Min(1) @Max(11) Short grupaVelicina,
        @NotNull @Min(1) @Max(20) Short casovaNedeljno,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina
) {}
