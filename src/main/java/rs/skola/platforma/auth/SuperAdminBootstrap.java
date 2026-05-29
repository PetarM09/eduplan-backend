package rs.skola.platforma.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements CommandLineRunner {

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.superadmin.username:superadmin}")
    private String username;

    @Value("${app.bootstrap.superadmin.email:superadmin@platforma.rs}")
    private String email;

    @Value("${app.bootstrap.superadmin.lozinka:Promeni-Me-Odmah-2026!}")
    private String lozinka;

    @Override
    @Transactional
    public void run(String... args) {
        if (korisnikRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        Korisnik admin = Korisnik.builder()
                .skola(null)
                .username(username)
                .email(email)
                .lozinkaHash(passwordEncoder.encode(lozinka))
                .ime("Super")
                .prezime("Admin")
                .uloga(Uloga.SUPER_ADMIN)
                .aktivan(true)
                .build();
        korisnikRepository.save(admin);
        log.warn("Kreiran je pocetni SUPER_ADMIN nalog '{}'. PROMENI LOZINKU IMMEDIJATELNO U PRODUKCIJI!", username);
    }
}
