package rs.skola.platforma.pp.web;

import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.domain.PPStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PPIzvestajResponse(
        UUID id,
        UUID staresinaId,
        String staresinaIme,
        UUID odeljenjeId,
        String odeljenjeLabel,
        PPPeriod period,
        String skolskaGodina,
        Map<String, Object> podaci,
        PPStatus status,
        OffsetDateTime podnetAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
