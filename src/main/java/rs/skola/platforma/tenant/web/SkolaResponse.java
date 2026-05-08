package rs.skola.platforma.tenant.web;

import java.util.UUID;

public record SkolaResponse(
        UUID id,
        String naziv,
        String grad,
        String adresa,
        String mailPlanovi,
        boolean aktivan
) {}
