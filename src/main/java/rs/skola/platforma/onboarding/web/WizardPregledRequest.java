package rs.skola.platforma.onboarding.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record WizardPregledRequest(
        @NotNull UUID tipSkoleId,
        @NotEmpty List<UUID> obrazovniProfiliIds
) {}
