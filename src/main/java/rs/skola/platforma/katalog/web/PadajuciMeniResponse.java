package rs.skola.platforma.katalog.web;

import java.util.UUID;

public record PadajuciMeniResponse(
        UUID id,
        String naziv,
        boolean sistemski
) {}
