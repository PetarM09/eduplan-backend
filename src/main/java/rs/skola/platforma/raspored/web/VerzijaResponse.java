package rs.skola.platforma.raspored.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VerzijaResponse(
        UUID id,
        String naziv,
        String skolskaGodina,
        LocalDate datumOd,
        boolean aktivan,
        long brojStavki,
        OffsetDateTime createdAt
) {}
