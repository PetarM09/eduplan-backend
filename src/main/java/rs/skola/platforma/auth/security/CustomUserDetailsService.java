package rs.skola.platforma.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final KorisnikRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Korisnik k = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik %s ne postoji".formatted(username)));
        return new CustomUserDetails(
                k.getId(),
                k.getSkola() == null ? null : k.getSkola().getId(),
                k.getUsername(),
                k.getLozinkaHash(),
                k.getEmail(),
                k.getUloga(),
                k.isAktivan()
        );
    }
}
