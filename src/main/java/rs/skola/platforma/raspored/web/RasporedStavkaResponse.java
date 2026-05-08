package rs.skola.platforma.raspored.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.UUID;

public record RasporedStavkaResponse(
        UUID id,
        Dan dan,
        Short cas,
        UUID korisnikId,
        String korisnikIme,
        UUID odeljenjeId,
        String odeljenjeLabel,
        String predmetLabel
) {}
