package rs.skola.platforma.korisnici;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.korisnici.web.KorisnikMapper;
import rs.skola.platforma.korisnici.web.KorisnikResponse;
import rs.skola.platforma.korisnici.web.KreirajKorisnikaRequest;
import rs.skola.platforma.raspored.RasporedService;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.util.List;
import java.util.UUID;

@Service
public class KorisnikService {

    private final KorisnikRepository korisnikRepository;
    private final SkolaRepository skolaRepository;
    private final KorisnikMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RasporedService rasporedService;

    @Autowired
    public KorisnikService(KorisnikRepository korisnikRepository,
                           SkolaRepository skolaRepository,
                           KorisnikMapper mapper,
                           PasswordEncoder passwordEncoder,
                           @Lazy RasporedService rasporedService) {
        this.korisnikRepository = korisnikRepository;
        this.skolaRepository = skolaRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.rasporedService = rasporedService;
    }

    @Transactional(readOnly = true)
    public List<KorisnikResponse> sviKorisniciSkole() {
        UUID skolaId = TenantContext.require();
        return korisnikRepository.findAllBySkolaIdOrderByPrezimeAscImeAsc(skolaId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KorisnikResponse> sviPoUlozi(Uloga uloga) {
        UUID skolaId = TenantContext.require();
        return korisnikRepository.findAllBySkolaIdAndUlogaOrderByPrezimeAscImeAsc(skolaId, uloga).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public KorisnikResponse kreirajKorisnika(KreirajKorisnikaRequest req) {
        if (req.uloga() == Uloga.SUPER_ADMIN) {
            throw new ValidationException("SUPER_ADMIN se ne moze kreirati kroz ovaj endpoint");
        }
        if (req.uloga() == Uloga.KOORDINATOR) {
            throw new ValidationException("KOORDINATOR-a kreira SUPER_ADMIN kroz /super/skole/{id}/koordinator");
        }

        UUID skolaId = TenantContext.require();
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
                .uloga(req.uloga())
                .aktivan(true)
                .build();
        Korisnik sacuvan = korisnikRepository.save(k);
        if (sacuvan.getUloga() == Uloga.NASTAVNIK || sacuvan.getUloga() == Uloga.KOORDINATOR) {
            rasporedService.autoMapirajZaKorisnika(sacuvan);
        }
        return mapper.toResponse(sacuvan);
    }

    @Transactional
    public KorisnikResponse deaktiviraj(UUID korisnikId) {
        Korisnik k = nadjiUTrenutnojSkoli(korisnikId);
        if (k.getUloga() == Uloga.KOORDINATOR) {
            throw new ValidationException("KOORDINATOR-a deaktivira SUPER_ADMIN");
        }
        k.setAktivan(false);
        return mapper.toResponse(k);
    }

    @Transactional(readOnly = true)
    public KorisnikResponse pregled(UUID korisnikId) {
        return mapper.toResponse(nadjiUTrenutnojSkoli(korisnikId));
    }

    private Korisnik nadjiUTrenutnojSkoli(UUID korisnikId) {
        UUID skolaId = TenantContext.require();
        Korisnik k = korisnikRepository.findById(korisnikId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", korisnikId));
        if (k.getSkola() == null || !skolaId.equals(k.getSkola().getId())) {
            throw new TenantViolationException();
        }
        return k;
    }
}
