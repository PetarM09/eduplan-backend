package rs.skola.platforma.zamene.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DodeliZamenikaRequest(
        @NotNull UUID zamenikId,
        @Size(max = 1000) String napomena
) {}
