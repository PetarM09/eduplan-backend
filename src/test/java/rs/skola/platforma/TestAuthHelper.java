package rs.skola.platforma;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.auth.security.JwtTokenProvider;
import rs.skola.platforma.korisnici.domain.Korisnik;

/**
 * Generise validan access token za zadati korisnik — testovi mogu da naprave Authorization
 * header bez prolaska kroz /api/v1/auth/login.
 */
@Component
@RequiredArgsConstructor
public class TestAuthHelper {

    private final JwtTokenProvider jwtProvider;

    public String bearerHeader(Korisnik k) {
        return "Bearer " + accessToken(k);
    }

    public String accessToken(Korisnik k) {
        CustomUserDetails ud = new CustomUserDetails(
                k.getId(),
                k.getSkola() == null ? null : k.getSkola().getId(),
                k.getUsername(),
                k.getLozinkaHash(),
                k.getEmail(),
                k.getUloga(),
                k.isAktivan()
        );
        return jwtProvider.generateAccessToken(ud).token();
    }
}
