package rs.skola.platforma.auth.web;

import java.time.Instant;
import java.util.UUID;

public record TokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID korisnikId,
        UUID skolaId,
        String username,
        String email,
        String uloga
) {}
