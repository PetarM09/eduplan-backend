package rs.skola.platforma.rotacija.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record KreirajRotacijuRequest(
        @NotBlank @Size(max = 255) String naziv,
        @NotNull UUID odeljenjeId,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina,
        @NotNull @Min(2) @Max(12) Short brojGrupa,
        @NotNull @Min(1) @Max(52) Short brojNedelja,
        @NotEmpty @Valid List<PredmetStavka> predmeti
) {

    public record PredmetStavka(
            @NotNull UUID profesorId,
            @NotBlank @Size(max = 255) String naziv,
            @NotNull @Min(1) @Max(20) Short casovaNedeljno
    ) {}
}
