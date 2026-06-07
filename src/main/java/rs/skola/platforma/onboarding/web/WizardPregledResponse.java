package rs.skola.platforma.onboarding.web;

import java.util.List;

public record WizardPregledResponse(
        List<PredmetUPregledu> predmeti,
        List<String> upozorenja
) {
    public record PredmetUPregledu(
            Short razred,
            String naziv,
            Short fondTeorija,
            Short fondVezbe,
            Short fondBlok,
            boolean vecPostoji
    ) {}
}
