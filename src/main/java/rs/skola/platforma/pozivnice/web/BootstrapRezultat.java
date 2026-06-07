package rs.skola.platforma.pozivnice.web;

import java.util.List;

public record BootstrapRezultat(
        int novihKorisnika,
        int preskocenih,
        List<String> upozorenja
) {}
