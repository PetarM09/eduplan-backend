package rs.skola.platforma.tenant.web;

import java.time.LocalDate;
import java.util.UUID;

public record SkolaResponse(
        UUID id,
        String naziv,
        String grad,
        String adresa,
        String mailPlanovi,
        boolean aktivan,
        LocalDate vaziDo
) {}
