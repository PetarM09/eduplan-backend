package rs.skola.platforma.predmeti.web;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record DodeliOdeljenjaRequest(
        @NotNull Set<UUID> odeljenjaIds
) {}
