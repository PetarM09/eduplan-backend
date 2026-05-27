package rs.skola.platforma.odeljenja;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.odeljenja.web.KreirajOdeljenjeRequest;
import rs.skola.platforma.odeljenja.web.OdeljenjeResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OdeljenjeService {

    private final OdeljenjeRepository odeljenjeRepo;
    private final KorisnikRepository korisnikRepo;

    @Transactional(readOnly = true)
    public List<OdeljenjeResponse> svaOdeljenjaSkole() {
        UUID skolaId = TenantContext.require();
        return odeljenjeRepo.findAllBySkolaIdOrderByRazredAscOznakaAsc(skolaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OdeljenjeResponse kreiraj(KreirajOdeljenjeRequest req) {
        UUID skolaId = TenantContext.require();
        
        Korisnik staresina = null;
        if (req.staresinaId() != null) {
            staresina = korisnikRepo.findById(req.staresinaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Korisnik staresina", req.staresinaId()));
            if (staresina.getSkola() == null || !skolaId.equals(staresina.getSkola().getId())) {
                throw new TenantViolationException("Izabrani staresina ne pripada vasoj skoli");
            }
        }

        Odeljenje o = Odeljenje.builder()
                .razred(req.razred())
                .oznaka(req.oznaka())
                .skolskaGodina(req.skolskaGodina())
                .staresina(staresina)
                .aktivan(true)
                .build();
        o.setSkolaId(skolaId);
        o = odeljenjeRepo.save(o);
        return toResponse(o);
    }

    @Transactional
    public OdeljenjeResponse postaviStaresinu(UUID id, UUID staresinaId) {
        UUID skolaId = TenantContext.require();
        Odeljenje o = odeljenjeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", id));
        if (!skolaId.equals(o.getSkolaId())) {
            throw new TenantViolationException();
        }

        Korisnik staresina = null;
        if (staresinaId != null) {
            staresina = korisnikRepo.findById(staresinaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Korisnik staresina", staresinaId));
            if (staresina.getSkola() == null || !skolaId.equals(staresina.getSkola().getId())) {
                throw new TenantViolationException("Izabrani staresina ne pripada vasoj skoli");
            }
        }

        o.setStaresina(staresina);
        o = odeljenjeRepo.save(o);
        return toResponse(o);
    }

    @Transactional
    public void deaktiviraj(UUID id) {
        UUID skolaId = TenantContext.require();
        Odeljenje o = odeljenjeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", id));
        if (!skolaId.equals(o.getSkolaId())) {
            throw new TenantViolationException();
        }
        o.setAktivan(false);
        odeljenjeRepo.save(o);
    }

    @Transactional
    public void obrisi(UUID id) {
        UUID skolaId = TenantContext.require();
        Odeljenje o = odeljenjeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", id));
        if (!skolaId.equals(o.getSkolaId())) {
            throw new TenantViolationException();
        }
        try {
            odeljenjeRepo.delete(o);
            odeljenjeRepo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "Odeljenje ima vezane planove, izvestaje ili stavke rasporeda. " +
                            "Deaktiviraj ga umesto brisanja.");
        }
    }

    private OdeljenjeResponse toResponse(Odeljenje o) {
        return new OdeljenjeResponse(
                o.getId(),
                o.getRazred(),
                o.getOznaka(),
                o.getSkolskaGodina(),
                o.getStaresina() != null ? o.getStaresina().getId() : null,
                o.getStaresina() != null ? o.getStaresina().punoIme() : null,
                o.isAktivan(),
                o.label()
        );
    }
}
