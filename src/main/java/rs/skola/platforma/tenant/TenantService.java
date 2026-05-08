package rs.skola.platforma.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.korisnici.web.KorisnikMapper;
import rs.skola.platforma.korisnici.web.KorisnikResponse;
import rs.skola.platforma.korisnici.web.KreirajKorisnikaRequest;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;
import rs.skola.platforma.tenant.web.KreirajSkoluRequest;
import rs.skola.platforma.tenant.web.SkolaMapper;
import rs.skola.platforma.tenant.web.SkolaResponse;

import java.util.List;
import java.util.UUID;

/**
 * Servis koristi iskljucivo SUPER_ADMIN — kreira skole i njihove koordinatore.
 * Sigurnosno ogranicenje je u {@code SuperAdminController} kroz {@code @PreAuthorize("hasRole('SUPER_ADMIN')")}.
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final SkolaRepository skolaRepository;
    private final KorisnikRepository korisnikRepository;
    private final SkolaMapper skolaMapper;
    private final KorisnikMapper korisnikMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<SkolaResponse> sveSkole() {
        return skolaRepository.findAllByOrderByNazivAsc().stream()
                .map(skolaMapper::toResponse)
                .toList();
    }

    @Transactional
    public SkolaResponse kreirajSkolu(KreirajSkoluRequest req) {
        String grad = req.grad() == null ? "" : req.grad();
        if (skolaRepository.existsByNazivIgnoreCaseAndGradIgnoreCase(req.naziv(), grad)) {
            throw new ConflictException("Skola sa istim nazivom i gradom vec postoji");
        }
        Skola s = Skola.builder()
                .naziv(req.naziv())
                .grad(req.grad())
                .adresa(req.adresa())
                .mailPlanovi(req.mailPlanovi())
                .aktivan(true)
                .build();
        return skolaMapper.toResponse(skolaRepository.save(s));
    }

    @Transactional
    public KorisnikResponse kreirajKoordinatora(UUID skolaId, KreirajKorisnikaRequest req) {
        if (req.uloga() != Uloga.KOORDINATOR) {
            throw new ConflictException("Ovaj endpoint kreira samo KOORDINATOR-a");
        }
        Skola skola = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));

        if (korisnikRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictException("Username vec postoji");
        }
        if (korisnikRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email vec postoji");
        }

        Korisnik k = Korisnik.builder()
                .skola(skola)
                .username(req.username())
                .email(req.email())
                .lozinkaHash(passwordEncoder.encode(req.lozinka()))
                .ime(req.ime())
                .prezime(req.prezime())
                .uloga(Uloga.KOORDINATOR)
                .aktivan(true)
                .build();
        return korisnikMapper.toResponse(korisnikRepository.save(k));
    }
}
