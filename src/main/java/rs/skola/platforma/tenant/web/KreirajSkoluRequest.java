package rs.skola.platforma.tenant.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record KreirajSkoluRequest(
        @NotBlank @Size(max = 255) String naziv,
        @Size(max = 100) String grad,
        @Size(max = 255) String adresa,
        LocalDate vaziDo
) {}
