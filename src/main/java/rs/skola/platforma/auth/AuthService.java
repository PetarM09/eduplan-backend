package rs.skola.platforma.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.domain.RefreshToken;
import rs.skola.platforma.auth.repo.RefreshTokenRepository;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.auth.security.JwtTokenProvider;
import rs.skola.platforma.auth.web.LoginRequest;
import rs.skola.platforma.auth.web.RefreshRequest;
import rs.skola.platforma.auth.web.TokenPair;
import rs.skola.platforma.common.exception.UnauthorizedException;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KorisnikRepository korisnikRepository;

    @Transactional
    public TokenPair login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            CustomUserDetails ud = (CustomUserDetails) auth.getPrincipal();

            Korisnik k = korisnikRepository.findById(ud.id())
                    .orElseThrow(() -> new UnauthorizedException("Nalog ne postoji"));
            k.setPoslednjiLogin(OffsetDateTime.now());

            return izdajPar(ud);
        } catch (BadCredentialsException ex) {
            log.info("Neuspeli login za '{}'", req.username());
            throw ex;
        }
    }

    @Transactional
    public TokenPair refresh(RefreshRequest req) {
        String hash = jwtProvider.sha256(req.refreshToken());
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token ne postoji"));

        if (!rt.isValid(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token je istekao ili opozvan");
        }

        Korisnik k = rt.getKorisnik();
        if (!k.isAktivan()) {
            throw new UnauthorizedException("Nalog je deaktiviran");
        }

        // Rotiramo refresh token: stari postaje revoked, klijent dobija novi.
        rt.setRevoked(true);

        CustomUserDetails ud = new CustomUserDetails(
                k.getId(),
                k.getSkola() == null ? null : k.getSkola().getId(),
                k.getUsername(),
                k.getLozinkaHash(),
                k.getEmail(),
                k.getUloga(),
                k.isAktivan()
        );
        return izdajPar(ud);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(jwtProvider.sha256(refreshToken))
                .ifPresent(rt -> rt.setRevoked(true));
    }

    private TokenPair izdajPar(CustomUserDetails ud) {
        JwtTokenProvider.AccessTokenIssued access = jwtProvider.generateAccessToken(ud);
        JwtTokenProvider.RefreshTokenIssued refresh = jwtProvider.generateRefreshToken();

        Korisnik korisnikRef = korisnikRepository.getReferenceById(ud.id());
        refreshTokenRepository.save(RefreshToken.builder()
                .korisnik(korisnikRef)
                .tokenHash(refresh.tokenHash())
                .expiresAt(refresh.expiration().atOffset(ZoneOffset.UTC))
                .revoked(false)
                .build());

        return new TokenPair(
                access.token(), access.expiration(),
                refresh.rawToken(), refresh.expiration(),
                ud.id(), ud.skolaId(), ud.username(), ud.email(),
                ud.uloga().name()
        );
    }
}
