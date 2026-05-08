package rs.skola.platforma.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.skola.platforma.common.tenant.TenantContext;

import java.io.IOException;
import java.util.List;

/**
 * Validira Bearer JWT, postavlja SecurityContext i — kriticno — TenantContext
 * sa skola_id claim-om.
 *
 * <p>TenantContext.clear() je u finally bloku tako da nikad ne moze "procuriti"
 * iz jednog zahteva u sledeci kada Tomcat thread (ili virtual thread) bude reusovan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                tryAuthenticate(token, request);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void tryAuthenticate(String token, HttpServletRequest request) {
        try {
            JwtTokenProvider.ParsedToken parsed = jwtProvider.parseAccessToken(token);
            CustomUserDetails ud = new CustomUserDetails(
                    parsed.userId(),
                    parsed.skolaId(),
                    parsed.username(),
                    "",
                    null,
                    parsed.uloga(),
                    true
            );
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    ud, null,
                    List.of(new SimpleGrantedAuthority(parsed.uloga().authority()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            if (parsed.skolaId() != null) {
                TenantContext.set(parsed.skolaId());
            }
        } catch (JwtException ex) {
            log.debug("Odbacen JWT na {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            // Ne podizemo izuzetak — security chain ce ovo videti kao anonimnog
            // korisnika i odgovoriti 401 ako je ruta zasticena.
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
