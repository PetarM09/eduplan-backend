package rs.skola.platforma.rotacija.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

/**
 * Zahtev za kreiranje rotacije.
 * Predmeti se grupisu po profesoru — za istog profesora moze biti vise stavki
 * (npr. Programski jezici 2 casa + Web 2 casa), suma se proverava nasuprot
 * detektovanim casovima vezbi iz rasporeda.
 */
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
