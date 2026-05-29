package rs.skola.platforma.pp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import rs.skola.platforma.pp.domain.PPPeriod;

import java.util.Map;
import java.util.UUID;

public record PPIzvestajRequest(
        @NotNull UUID odeljenjeId,
        @NotNull PPPeriod period,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}") String skolskaGodina,
        Map<String, Object> podaci
) {}
