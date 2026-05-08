package rs.skola.platforma.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rs.skola.platforma.korisnici.domain.Uloga;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security UserDetails wrapper. Pohranjuje sve sto JwtTokenProvider treba
 * da unese u JWT — id, skolaId, ulogu — da se izbegne dodatni DB poziv pri svakom zahtevu.
 */
public record CustomUserDetails(
        UUID id,
        UUID skolaId,
        String username,
        String passwordHash,
        String email,
        Uloga uloga,
        boolean aktivan
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(uloga.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return aktivan;
    }

    @Override
    public boolean isAccountNonLocked() {
        return aktivan;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return aktivan;
    }

    @Override
    public boolean isEnabled() {
        return aktivan;
    }
}
