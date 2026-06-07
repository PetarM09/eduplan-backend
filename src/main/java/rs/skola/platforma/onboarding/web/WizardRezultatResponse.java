package rs.skola.platforma.onboarding.web;

import java.util.List;

public record WizardRezultatResponse(
        int novihPredmeta,
        int preskocenihPredmeta,
        int novihOdeljenja,
        int preskocenihOdeljenja,
        List<String> upozorenja
) {}
