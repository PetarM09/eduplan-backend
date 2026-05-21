package rs.skola.platforma.pp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import rs.skola.platforma.pp.domain.PPPeriod;

import java.util.Map;
import java.util.UUID;

/**
 * Sadrzaj izvestaja je free-form JSON sa preporucenom strukturom:
 * <pre>{@code
 * {
 *   "ukupnoUcenika": 28,
 *   "ucenikaMuski": 16,
 *   "ucenikaZenski": 12,
 *   "prisustvo": {"opravdana": 124, "neopravdana": 8},
 *   "vladanje": {"primerno": 22, "vrloDobro": 4, "dobro": 2, "zadovoljavajuce": 0, "nezadovoljavajuce": 0},
 *   "uspeh": {"odlican": 9, "vrloDobar": 11, "dobar": 6, "dovoljan": 2, "nedovoljan": 0},
 *   "problemi": "Slobodan tekst...",
 *   "mere": "Slobodan tekst..."
 * }
 * }</pre>
 * StatistikaAggregatorService agregira po ovim kljucevima — svaki je opcioni.
 */
public record PPIzvestajRequest(
        @NotNull UUID odeljenjeId,
        @NotNull PPPeriod period,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina,
        Map<String, Object> podaci
) {}
