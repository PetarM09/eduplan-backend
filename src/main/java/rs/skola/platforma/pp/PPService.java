package rs.skola.platforma.pp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.pp.domain.PPIzvestaj;
import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.domain.PPStatus;
import rs.skola.platforma.pp.repo.PPIzvestajRepository;
import rs.skola.platforma.pp.web.PPIzvestajRequest;
import rs.skola.platforma.pp.web.PPIzvestajResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Upravljanje PP izvestajima staresina. Idempotent po (skola, odeljenje, period, godina).
 */
@Service
@RequiredArgsConstructor
public class PPService {

    private final PPIzvestajRepository repo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final KorisnikRepository korisnikRepo;

    @Transactional
    public PPIzvestajResponse kreirajIliAzuriraj(CustomUserDetails ja, PPIzvestajRequest req) {
        UUID skolaId = TenantContext.require();
        Odeljenje odeljenje = nadjiOdeljenje(req.odeljenjeId(), skolaId);
        if (odeljenje.getStaresina() == null || !odeljenje.getStaresina().getId().equals(ja.id())) {
            throw new TenantViolationException("Izvestaj moze podneti samo staresina odeljenja");
        }

        PPIzvestaj izv = repo
                .findBySkolaIdAndOdeljenje_IdAndPeriodAndSkolskaGodina(
                        skolaId, odeljenje.getId(), req.period(), req.skolskaGodina())
                .orElseGet(() -> {
                    PPIzvestaj nov = PPIzvestaj.builder()
                            .staresina(korisnikRepo.getReferenceById(ja.id()))
                            .odeljenje(odeljenje)
                            .period(req.period())
                            .skolskaGodina(req.skolskaGodina())
                            .status(PPStatus.NACRT)
                            .build();
                    nov.setSkolaId(skolaId);
                    return nov;
                });

        if (izv.getStatus() == PPStatus.PRIHVACEN) {
            throw new ConflictException("Prihvacen izvestaj se ne moze menjati");
        }
        izv.setPodaci(req.podaci());
        return toResponse(repo.save(izv));
    }

    @Transactional
    public PPIzvestajResponse podnesi(UUID id, CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        PPIzvestaj izv = nadji(id, skolaId);
        if (izv.getStaresina() == null || !izv.getStaresina().getId().equals(ja.id())) {
            throw new TenantViolationException("Izvestaj moze podneti samo staresina");
        }
        izv.setStatus(PPStatus.PODNET);
        izv.setPodnetAt(OffsetDateTime.now());
        return toResponse(izv);
    }

    @Transactional
    public PPIzvestajResponse prihvati(UUID id) {
        UUID skolaId = TenantContext.require();
        PPIzvestaj izv = nadji(id, skolaId);
        if (izv.getStatus() != PPStatus.PODNET) {
            throw new ConflictException("Samo PODNET izvestaj se moze prihvatiti");
        }
        izv.setStatus(PPStatus.PRIHVACEN);
        return toResponse(izv);
    }

    @Transactional
    public PPIzvestajResponse vratiNaDoradu(UUID id) {
        UUID skolaId = TenantContext.require();
        PPIzvestaj izv = nadji(id, skolaId);
        if (izv.getStatus() != PPStatus.PODNET) {
            throw new ConflictException("Samo PODNET izvestaj moze biti vracen na doradu");
        }
        izv.setStatus(PPStatus.VRACENO_NA_DORADU);
        return toResponse(izv);
    }

    @Transactional(readOnly = true)
    public List<PPIzvestajResponse> mojiIzvestaji(CustomUserDetails ja, String skolskaGodina) {
        UUID skolaId = TenantContext.require();
        return repo.mojiIzvestaji(skolaId, ja.id(), skolskaGodina).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PPIzvestajResponse> sviZaSkolu(String skolskaGodina, PPPeriod period, PPStatus status) {
        UUID skolaId = TenantContext.require();
        return repo.sviZaSkolu(skolaId, skolskaGodina, period, status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PPIzvestajResponse pregled(UUID id) {
        UUID skolaId = TenantContext.require();
        return toResponse(nadji(id, skolaId));
    }

    private PPIzvestaj nadji(UUID id, UUID skolaId) {
        PPIzvestaj i = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PP izvestaj", id));
        if (!skolaId.equals(i.getSkolaId())) {
            throw new TenantViolationException();
        }
        return i;
    }

    private Odeljenje nadjiOdeljenje(UUID id, UUID skolaId) {
        Odeljenje o = odeljenjeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", id));
        if (!skolaId.equals(o.getSkolaId())) {
            throw new TenantViolationException();
        }
        return o;
    }

    private PPIzvestajResponse toResponse(PPIzvestaj i) {
        return new PPIzvestajResponse(
                i.getId(),
                i.getStaresina().getId(),
                i.getStaresina().punoIme(),
                i.getOdeljenje().getId(),
                i.getOdeljenje().label(),
                i.getPeriod(),
                i.getSkolskaGodina(),
                i.getPodaci(),
                i.getStatus(),
                i.getPodnetAt(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}
