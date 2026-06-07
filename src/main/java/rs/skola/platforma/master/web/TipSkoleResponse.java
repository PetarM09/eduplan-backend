package rs.skola.platforma.master.web;

import java.util.UUID;

public record TipSkoleResponse(
        UUID id,
        String kod,
        String naziv,
        Short ukupnoRazreda,
        long brojProfila
) {}
