package rs.skola.platforma.master.web;

import java.util.UUID;

public record MasterPredmetResponse(
        UUID id,
        UUID obrazovniProfilId,
        Short razred,
        String naziv,
        Short fondTeorija,
        Short fondVezbe,
        Short fondBlok,
        Boolean obavezan,
        Short redosled
) {}
