package rs.skola.platforma.master.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KreirajObrazovniProfilRequest(
        @NotNull UUID tipSkoleId,
        @NotBlank String kod,
        @NotBlank String naziv,
        String opis
) {}
