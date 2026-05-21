package rs.skola.platforma.rotacija.web;

import java.util.UUID;

public record OdeljenjeKratko(
        UUID id,
        String label,
        Short razred,
        String oznaka
) {}
