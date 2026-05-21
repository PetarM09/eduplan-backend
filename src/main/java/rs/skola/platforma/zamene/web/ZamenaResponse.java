package rs.skola.platforma.zamene.web;

import rs.skola.platforma.zamene.domain.ZamenaStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ZamenaResponse(
        UUID id,
        LocalDate datum,
        Short cas,
        UUID odsutniId,
        String odsutniIme,
        UUID zamenikId,
        String zamenikIme,
        UUID odeljenjeId,
        String odeljenjeLabel,
        String predmetLabel,
        String razlog,
        String napomena,
        ZamenaStatus status,
        UUID odobrioId,
        String odobrioIme,
        OffsetDateTime odobrioAt,
        OffsetDateTime createdAt
) {}
