package rs.skola.platforma.pozivnice.web;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PostaviPredmeteRequest(
        @NotNull List<UUID> predmetiIds
) {}
