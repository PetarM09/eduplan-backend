package rs.skola.platforma.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.skola.platforma.korisnici.domain.Uloga;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_SKOLA_ID = "sid";
    public static final String CLAIM_ULOGA = "rol";
    public static final String CLAIM_TYPE = "typ";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;

    private SecretKey signingKey() {
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret mora biti najmanje 32 karaktera dugacak");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public AccessTokenIssued generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(props.accessTokenExpirationMinutes(), ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(user.username())
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USER_ID, user.id().toString())
                .claim(CLAIM_SKOLA_ID, user.skolaId() == null ? null : user.skolaId().toString())
                .claim(CLAIM_ULOGA, user.uloga().name())
                .signWith(signingKey())
                .compact();
        return new AccessTokenIssued(token, expiration);
    }

    public RefreshTokenIssued generateRefreshToken() {
        Instant now = Instant.now();
        Instant expiration = now.plus(props.refreshTokenExpirationDays(), ChronoUnit.DAYS);
        // 32 bajta = 256-bitni opaque token (urlBase64 kodiran)
        String raw = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        return new RefreshTokenIssued(raw, sha256(raw), expiration);
    }

    public Claims parseAndVerify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            log.debug("JWT parse fail: {}", ex.getMessage());
            throw ex;
        }
    }

    public ParsedToken parseAccessToken(String token) {
        Claims claims = parseAndVerify(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Token nije access tipa");
        }
        UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
        String sidStr = claims.get(CLAIM_SKOLA_ID, String.class);
        UUID skolaId = sidStr == null ? null : UUID.fromString(sidStr);
        Uloga uloga = Uloga.valueOf(claims.get(CLAIM_ULOGA, String.class));
        return new ParsedToken(claims.getSubject(), userId, skolaId, uloga, claims.getExpiration().toInstant());
    }

    public String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nije dostupan na ovoj JVM-i", ex);
        }
    }

    public record AccessTokenIssued(String token, Instant expiration) {}

    public record RefreshTokenIssued(String rawToken, String tokenHash, Instant expiration) {}

    public record ParsedToken(String username, UUID userId, UUID skolaId, Uloga uloga, Instant expiration) {}
}
