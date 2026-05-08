package rs.skola.platforma.raspored.parser;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.List;

public record ParsedRasporedRed(
        String nastavnikLabel,
        List<ParsedStavka> stavke
) {

    public record ParsedStavka(
            Dan dan,
            short cas,
            String odeljenjeLabel
    ) {}
}
