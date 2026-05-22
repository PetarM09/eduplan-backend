package rs.skola.platforma.odeljenja.web;

import java.util.UUID;

public record OdeljenjeResponse(
        UUID id,
        short razred,
        String oznaka,
        String skolskaGodina,
        UUID staresinaId,
        String staresinaIme,
        boolean aktivan,
        String label
) {}
