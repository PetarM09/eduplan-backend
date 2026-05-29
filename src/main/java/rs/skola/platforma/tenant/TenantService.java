package rs.skola.platforma.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.ValidationException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
                .aktivan(true)
                .vaziDo(req.vaziDo())
                .build();
        return skolaMapper.toResponse(skolaRepository.save(s));
    }

    @Transactional
    public SkolaResponse aktivirajSkolu(UUID skolaId) {
        Skola s = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        s.setAktivan(true);
        return skolaMapper.toResponse(s);
    }

    @Transactional
    public SkolaResponse deaktivirajSkolu(UUID skolaId) {
        Skola s = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        s.setAktivan(false);
        return skolaMapper.toResponse(s);
    }

    @Transactional
    public SkolaResponse postaviVaziDo(UUID skolaId, LocalDate vaziDo) {
        Skola s = skolaRepository.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        s.setVaziDo(vaziDo);
        return skolaMapper.toResponse(s);
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

    @Transactional(readOnly = true)
    public List<KorisnikResponse> korisniciSkole(UUID skolaId) {
        if (!skolaRepository.existsById(skolaId)) {
            throw new ResourceNotFoundException("Skola", skolaId);
        }
        return korisnikRepository.findAllBySkolaIdOrderByPrezimeAscImeAsc(skolaId).stream()
                .map(korisnikMapper::toResponse)
                .toList();
    }

    @Transactional
    public KorisnikResponse aktivirajKorisnika(UUID korisnikId) {
        Korisnik k = nadjiUnutarSkole(korisnikId);
        k.setAktivan(true);
        return korisnikMapper.toResponse(k);
    }

    @Transactional
    public KorisnikResponse deaktivirajKorisnika(UUID korisnikId) {
        Korisnik k = nadjiUnutarSkole(korisnikId);
        k.setAktivan(false);
        return korisnikMapper.toResponse(k);
    }

    @Transactional
    public KorisnikResponse promeniUlogu(UUID korisnikId, Uloga novaUloga) {
        if (novaUloga == Uloga.SUPER_ADMIN) {
            throw new ValidationException("Ne moze se dodeliti SUPER_ADMIN uloga unutar skole");
        }
        Korisnik k = nadjiUnutarSkole(korisnikId);
        k.setUloga(novaUloga);
        return korisnikMapper.toResponse(k);
    }

    @Transactional
    public void obrisiKorisnika(UUID korisnikId) {
        Korisnik k = nadjiUnutarSkole(korisnikId);
        try {
            korisnikRepository.delete(k);
            korisnikRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "Korisnik ima vezane planove, izvestaje ili druge zapise. " +
                            "Deaktiviraj ga umesto brisanja.");
        }
    }

    private Korisnik nadjiUnutarSkole(UUID korisnikId) {
        Korisnik k = korisnikRepository.findById(korisnikId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", korisnikId));
        if (k.getUloga() == Uloga.SUPER_ADMIN || k.getSkola() == null) {
            throw new ValidationException("Operacija nije dozvoljena nad SUPER_ADMIN nalogom");
        }
        return k;
    }
}
