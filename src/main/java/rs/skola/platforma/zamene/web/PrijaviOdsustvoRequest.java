package rs.skola.platforma.zamene.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

public record PrijaviOdsustvoRequest(
        @NotNull
        @Future(message = "Datum mora biti u buducnosti")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate datum,

        @NotEmpty(message = "Mora se navesti bar jedan cas")
        @Size(max = 8, message = "Najvise 8 casova po danu")
        Set<Short> casovi,

        @Size(max = 500)
        String razlog
) {}
