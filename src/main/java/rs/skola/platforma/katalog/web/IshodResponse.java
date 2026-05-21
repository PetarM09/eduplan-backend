package rs.skola.platforma.katalog.web;

import java.util.UUID;

public record IshodResponse(
        UUID id,
        UUID temaId,
        String opis
) {}
