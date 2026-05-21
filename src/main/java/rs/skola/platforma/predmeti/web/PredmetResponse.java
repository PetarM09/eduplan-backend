package rs.skola.platforma.predmeti.web;

import java.util.List;
import java.util.UUID;

public record PredmetResponse(
        UUID id,
        String naziv,
        Short razred,
        Short fondCasova,
        boolean aktivan,
        List<OdeljenjeKratko> odeljenja
) {

    public record OdeljenjeKratko(UUID id, String label) {}
}
