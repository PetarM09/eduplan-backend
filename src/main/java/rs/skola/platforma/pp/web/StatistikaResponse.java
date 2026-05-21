package rs.skola.platforma.pp.web;

import rs.skola.platforma.pp.domain.PPPeriod;

import java.util.Map;

public record StatistikaResponse(
        String skolskaGodina,
        PPPeriod period,
        int brojIzvestaja,
        long ukupnoUcenika,
        long ucenikaMuski,
        long ucenikaZenski,
        Prisustvo prisustvo,
        Map<String, Long> vladanjeDistribucija,
        Map<String, Long> uspehDistribucija
) {

    public record Prisustvo(long opravdana, long neopravdana) {}
}
