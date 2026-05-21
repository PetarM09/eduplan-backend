package rs.skola.platforma.rotacija.web;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AzurirajNedeljuRequest(
        @NotEmpty List<UUID> odeljenjaIds
) {}
