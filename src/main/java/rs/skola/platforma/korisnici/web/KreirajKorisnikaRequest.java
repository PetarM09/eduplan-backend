package rs.skola.platforma.korisnici.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import rs.skola.platforma.korisnici.domain.Uloga;

public record KreirajKorisnikaRequest(
        @NotBlank @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Username sme da sadrzi samo slova, brojeve i znakove . _ -")
        String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 200) String lozinka,
        @NotBlank @Size(max = 100) String ime,
        @NotBlank @Size(max = 100) String prezime,
        @NotNull Uloga uloga
) {}
