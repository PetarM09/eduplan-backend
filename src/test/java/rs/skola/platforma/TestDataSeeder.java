package rs.skola.platforma;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.util.UUID;

/**
 * Pomocnik za seeding test podataka. Sve metode kreiraju jedinstvene podatke
 * (UUID suffix u username/email) da testovi ne kolidiraju.
 */
@Component
@RequiredArgsConstructor
public class TestDataSeeder {

    public static final String DEFAULT_LOZINKA = "TestLozinka123";

    private final SkolaRepository skolaRepo;
    private final KorisnikRepository korisnikRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public Skola kreirajSkolu(String naziv) {
        return skolaRepo.save(Skola.builder()
                .naziv(naziv)
                .grad("Test")
                .mailPlanovi("planovi+%s@test.local".formatted(UUID.randomUUID()))
                .aktivan(true)
                .build());
    }

    @Transactional
    public Korisnik kreirajKorisnika(Skola skola, Uloga uloga) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Korisnik k = Korisnik.builder()
                .skola(skola)
                .username(uloga.name().toLowerCase() + "-" + suffix)
                .email("user-" + suffix + "@test.local")
                .lozinkaHash(encoder.encode(DEFAULT_LOZINKA))
                .ime("Test")
                .prezime(uloga.name())
                .uloga(uloga)
                .aktivan(true)
                .build();
        return korisnikRepo.save(k);
    }

    @Transactional
    public Korisnik kreirajSuperAdmina() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Korisnik k = Korisnik.builder()
                .skola(null)
                .username("sa-" + suffix)
                .email("sa-" + suffix + "@test.local")
                .lozinkaHash(encoder.encode(DEFAULT_LOZINKA))
                .ime("Super")
                .prezime("Admin")
                .uloga(Uloga.SUPER_ADMIN)
                .aktivan(true)
                .build();
        return korisnikRepo.save(k);
    }

    @Transactional
    public Odeljenje kreirajOdeljenje(Skola skola, short razred, String oznaka, Korisnik staresina) {
        Odeljenje o = Odeljenje.builder()
                .razred(razred)
                .oznaka(oznaka)
                .skolskaGodina("2024/2025")
                .staresina(staresina)
                .aktivan(true)
                .build();
        o.setSkolaId(skola.getId());
        return odeljenjeRepo.save(o);
    }
}
