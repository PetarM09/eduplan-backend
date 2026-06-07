package rs.skola.platforma.onboarding.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PokreniWizardRequest(
        @NotNull UUID tipSkoleId,
        @NotEmpty List<UUID> obrazovniProfiliIds,
        @NotEmpty List<RazredKonfig> razredi,
        @NotBlank String skolskaGodina
) {
    public record RazredKonfig(
            @NotNull Short razred,
            @NotEmpty @Size(max = 26) List<@NotBlank String> oznake
    ) {}
}
