package rs.skola.platforma.rotacija.web;

import java.util.List;
import java.util.UUID;

public record RotNedeljaResponse(
        UUID id,
        Short brojNedelje,
        List<OdeljenjeKratko> odeljenja
) {}
