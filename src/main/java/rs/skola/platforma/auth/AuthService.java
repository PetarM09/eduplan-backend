package rs.skola.platforma.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.domain.RefreshToken;
import rs.skola.platforma.auth.repo.RefreshTokenRepository;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.auth.security.JwtTokenProvider;
import rs.skola.platforma.auth.web.LoginRequest;
import rs.skola.platforma.auth.web.PromenaLozinkeRequest;
import rs.skola.platforma.auth.web.RefreshRequest;
import rs.skola.platforma.auth.web.TokenPair;
import rs.skola.platforma.common.exception.UnauthorizedException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.tenant.domain.Skola;

import java.time.LocalDate;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenPair login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            CustomUserDetails ud = (CustomUserDetails) auth.getPrincipal();

            Korisnik k = korisnikRepository.findById(ud.id())
                    .orElseThrow(() -> new UnauthorizedException("Nalog ne postoji"));
            proveriSkolu(k);
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
        proveriSkolu(k);

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
    public void promeniLozinku(CustomUserDetails ja, PromenaLozinkeRequest req) {
        Korisnik k = korisnikRepository.findById(ja.id())
                .orElseThrow(() -> new UnauthorizedException("Nalog ne postoji"));

        if (!passwordEncoder.matches(req.staraLozinka(), k.getLozinkaHash())) {
            throw new ValidationException("POGRESNA_LOZINKA", "Trenutna lozinka nije ispravna");
        }
        if (passwordEncoder.matches(req.novaLozinka(), k.getLozinkaHash())) {
            throw new ValidationException("ISTA_LOZINKA", "Nova lozinka mora biti razlicita od trenutne");
        }

        k.setLozinkaHash(passwordEncoder.encode(req.novaLozinka()));
        // Sve aktivne sesije na drugim uredjajima se opozivaju nakon promene lozinke.
        refreshTokenRepository.revokeAllForKorisnik(k.getId());
        log.info("Korisnik {} je promenio lozinku", k.getId());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(jwtProvider.sha256(refreshToken))
                .ifPresent(rt -> rt.setRevoked(true));
    }

    private void proveriSkolu(Korisnik k) {
        Skola s = k.getSkola();
        if (s == null) return; // SUPER_ADMIN
        if (!s.jeAktivnaNa(LocalDate.now())) {
            String razlog = !s.isAktivan()
                    ? "Skola je deaktivirana. Obrati se administratoru platforme."
                    : "Pristup skoli je istekao (%s). Obrati se administratoru platforme.".formatted(s.getVaziDo());
            throw new UnauthorizedException(razlog);
        }
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
