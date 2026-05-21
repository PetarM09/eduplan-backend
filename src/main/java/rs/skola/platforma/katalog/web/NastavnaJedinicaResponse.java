package rs.skola.platforma.katalog.web;

import java.util.UUID;

public record NastavnaJedinicaResponse(
        UUID id,
        UUID temaId,
        Short redniBroj,
        String naziv
) {}
