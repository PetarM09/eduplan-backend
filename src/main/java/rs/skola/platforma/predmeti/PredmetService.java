package rs.skola.platforma.predmeti;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;
import rs.skola.platforma.predmeti.web.DodeliOdeljenjaRequest;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PredmetService {

    private final PredmetRepository predmetRepo;
    private final OdeljenjeRepository odeljenjeRepo;

    @Transactional(readOnly = true)
    public List<PredmetResponse> sviAktivni() {
        UUID skolaId = TenantContext.require();
        return predmetRepo.findAllBySkolaIdAndAktivanTrueOrderByRazredAscNazivAsc(skolaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PredmetResponse> svi() {
        UUID skolaId = TenantContext.require();
        return predmetRepo.findAllBySkolaIdOrderByRazredAscNazivAsc(skolaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PredmetResponse pregled(UUID id) {
        return toResponse(nadji(id));
    }

    @Transactional
    public PredmetResponse kreiraj(KreirajPredmetRequest req) {
        UUID skolaId = TenantContext.require();
        if (predmetRepo.existsBySkolaIdAndNazivIgnoreCaseAndRazred(skolaId, req.naziv(), req.razred())) {
            throw new ConflictException("Predmet sa istim nazivom i razredom vec postoji");
        }
        Predmet p = Predmet.builder()
                .naziv(req.naziv())
                .razred(req.razred())
                .fondCasova(req.fondCasova())
                .aktivan(true)
                .build();
        p.setSkolaId(skolaId);
        return toResponse(predmetRepo.save(p));
    }

    @Transactional
    public PredmetResponse azuriraj(UUID id, KreirajPredmetRequest req) {
        Predmet p = nadji(id);
        p.setNaziv(req.naziv());
        p.setRazred(req.razred());
        p.setFondCasova(req.fondCasova());
        return toResponse(p);
    }

    @Transactional
    public PredmetResponse dodeliOdeljenja(UUID id, DodeliOdeljenjaRequest req) {
        UUID skolaId = TenantContext.require();
        Predmet p = nadji(id);

        Set<Odeljenje> nova = new HashSet<>(odeljenjeRepo.findAllById(req.odeljenjaIds()));
        if (nova.size() != req.odeljenjaIds().size()) {
            throw new ResourceNotFoundException("Jedno ili vise odeljenja ne postoje");
        }
        for (Odeljenje o : nova) {
            if (!skolaId.equals(o.getSkolaId())) {
                throw new TenantViolationException("Odeljenje " + o.label() + " ne pripada vasoj skoli");
            }
        }
        p.getOdeljenja().clear();
        p.getOdeljenja().addAll(nova);
        return toResponse(p);
    }

    @Transactional
    public void deaktiviraj(UUID id) {
        nadji(id).setAktivan(false);
    }

    private Predmet nadji(UUID id) {
        UUID skolaId = TenantContext.require();
        Predmet p = predmetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", id));
        if (!skolaId.equals(p.getSkolaId())) {
            throw new TenantViolationException();
        }
        return p;
    }

    private PredmetResponse toResponse(Predmet p) {
        List<PredmetResponse.OdeljenjeKratko> odeljenja = p.getOdeljenja().stream()
                .sorted(Comparator.comparing(Odeljenje::getRazred).thenComparing(Odeljenje::getOznaka))
                .map(o -> new PredmetResponse.OdeljenjeKratko(o.getId(), o.label()))
                .toList();
        return new PredmetResponse(
                p.getId(), p.getNaziv(), p.getRazred(), p.getFondCasova(), p.isAktivan(), odeljenja);
    }
}
