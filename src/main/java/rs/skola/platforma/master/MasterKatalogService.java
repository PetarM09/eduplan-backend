package rs.skola.platforma.master;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.master.domain.MasterPredmet;
import rs.skola.platforma.master.domain.ObrazovniProfil;
import rs.skola.platforma.master.domain.TipSkole;
import rs.skola.platforma.master.repo.MasterPredmetRepository;
import rs.skola.platforma.master.repo.ObrazovniProfilRepository;
import rs.skola.platforma.master.repo.TipSkoleRepository;
import rs.skola.platforma.master.web.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MasterKatalogService {

    private final TipSkoleRepository tipRepo;
    private final ObrazovniProfilRepository profilRepo;
    private final MasterPredmetRepository predmetRepo;

    // -------- TipSkole --------

    @Transactional(readOnly = true)
    public List<TipSkoleResponse> sviTipovi() {
        return tipRepo.findAllByOrderByNazivAsc().stream()
                .map(t -> new TipSkoleResponse(
                        t.getId(), t.getKod(), t.getNaziv(), t.getUkupnoRazreda(),
                        profilRepo.findAllByTipSkole_IdOrderByNazivAsc(t.getId()).size()
                ))
                .toList();
    }

    @Transactional
    public TipSkoleResponse kreirajTip(KreirajTipSkoleRequest req) {
        String kod = req.kod().trim().toUpperCase().replaceAll("\\s+", "_");
        tipRepo.findByKod(kod).ifPresent(t -> {
            throw new ConflictException("Tip skole sa kodom " + kod + " vec postoji");
        });
        TipSkole t = TipSkole.builder()
                .kod(kod)
                .naziv(req.naziv().trim())
                .ukupnoRazreda(req.ukupnoRazreda())
                .build();
        t = tipRepo.save(t);
        return toResponse(t);
    }

    @Transactional
    public void obrisiTip(UUID id) {
        TipSkole t = tipRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tip skole", id));
        if (!profilRepo.findAllByTipSkole_IdOrderByNazivAsc(id).isEmpty()) {
            throw new ConflictException("Tip skole ima vezane obrazovne profile — prvo ih obrisi");
        }
        tipRepo.delete(t);
    }

    // -------- ObrazovniProfil --------

    @Transactional(readOnly = true)
    public List<ObrazovniProfilResponse> sviProfili(UUID tipSkoleId) {
        List<ObrazovniProfil> profili = tipSkoleId == null
                ? profilRepo.findAllByOrderByNazivAsc()
                : profilRepo.findAllByTipSkole_IdOrderByNazivAsc(tipSkoleId);
        return profili.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ObrazovniProfilResponse kreirajProfil(KreirajObrazovniProfilRequest req) {
        TipSkole tip = tipRepo.findById(req.tipSkoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Tip skole", req.tipSkoleId()));
        String kod = req.kod().trim().toUpperCase().replaceAll("\\s+", "_");
        profilRepo.findByKod(kod).ifPresent(p -> {
            throw new ConflictException("Profil sa kodom " + kod + " vec postoji");
        });
        ObrazovniProfil p = ObrazovniProfil.builder()
                .tipSkole(tip)
                .kod(kod)
                .naziv(req.naziv().trim())
                .opis(req.opis())
                .build();
        p = profilRepo.save(p);
        return toResponse(p);
    }

    @Transactional
    public void obrisiProfil(UUID id) {
        ObrazovniProfil p = profilRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obrazovni profil", id));
        // Predmeti se brisu CASCADE-om na nivou DB.
        profilRepo.delete(p);
    }

    // -------- Predmeti profila --------

    @Transactional(readOnly = true)
    public List<MasterPredmetResponse> predmetiProfila(UUID profilId) {
        return predmetRepo.findAllByObrazovniProfil_IdOrderByRazredAscRedosledAscNazivAsc(profilId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MasterPredmetResponse kreirajPredmet(UUID profilId, KreirajMasterPredmetRequest req) {
        ObrazovniProfil profil = profilRepo.findById(profilId)
                .orElseThrow(() -> new ResourceNotFoundException("Obrazovni profil", profilId));
        validirajFond(req);
        validirajRazred(profil.getTipSkole(), req.razred());
        String naziv = req.naziv().trim();
        if (predmetRepo.existsByObrazovniProfil_IdAndRazredAndNaziv(profilId, req.razred(), naziv)) {
            throw new ConflictException("Predmet \"" + naziv + "\" vec postoji u " + req.razred() + ". razredu");
        }
        MasterPredmet p = MasterPredmet.builder()
                .obrazovniProfil(profil)
                .razred(req.razred())
                .naziv(naziv)
                .fondTeorija(req.fondTeorija())
                .fondVezbe(req.fondVezbe())
                .fondBlok(req.fondBlok())
                .obavezan(req.obavezan() == null ? Boolean.TRUE : req.obavezan())
                .redosled(req.redosled())
                .build();
        p = predmetRepo.save(p);
        return toResponse(p);
    }

    @Transactional
    public MasterPredmetResponse azurirajPredmet(UUID predmetId, KreirajMasterPredmetRequest req) {
        MasterPredmet p = predmetRepo.findById(predmetId)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", predmetId));
        validirajFond(req);
        validirajRazred(p.getObrazovniProfil().getTipSkole(), req.razred());
        String naziv = req.naziv().trim();
        if (!naziv.equals(p.getNaziv()) || !req.razred().equals(p.getRazred())) {
            if (predmetRepo.existsByObrazovniProfil_IdAndRazredAndNaziv(
                    p.getObrazovniProfil().getId(), req.razred(), naziv)) {
                throw new ConflictException("Predmet sa tim nazivom vec postoji u " + req.razred() + ". razredu");
            }
        }
        p.setRazred(req.razred());
        p.setNaziv(naziv);
        p.setFondTeorija(req.fondTeorija());
        p.setFondVezbe(req.fondVezbe());
        p.setFondBlok(req.fondBlok());
        p.setObavezan(req.obavezan() == null ? Boolean.TRUE : req.obavezan());
        p.setRedosled(req.redosled());
        return toResponse(p);
    }

    @Transactional
    public void obrisiPredmet(UUID predmetId) {
        MasterPredmet p = predmetRepo.findById(predmetId)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", predmetId));
        predmetRepo.delete(p);
    }

    // -------- helpers --------

    private void validirajFond(KreirajMasterPredmetRequest req) {
        int suma = req.fondTeorija() + req.fondVezbe() + req.fondBlok();
        if (suma <= 0) {
            throw new ValidationException("Bar jedan od T/V/B mora biti veci od 0");
        }
    }

    private void validirajRazred(TipSkole tip, Short razred) {
        if (razred < 1 || razred > tip.getUkupnoRazreda()) {
            throw new ValidationException("Razred " + razred + " nije validan za tip " + tip.getNaziv()
                    + " (dozvoljeno 1-" + tip.getUkupnoRazreda() + ")");
        }
    }

    private TipSkoleResponse toResponse(TipSkole t) {
        return new TipSkoleResponse(t.getId(), t.getKod(), t.getNaziv(), t.getUkupnoRazreda(),
                profilRepo.findAllByTipSkole_IdOrderByNazivAsc(t.getId()).size());
    }

    private ObrazovniProfilResponse toResponse(ObrazovniProfil p) {
        return new ObrazovniProfilResponse(
                p.getId(),
                p.getTipSkole().getId(),
                p.getTipSkole().getNaziv(),
                p.getTipSkole().getUkupnoRazreda(),
                p.getKod(),
                p.getNaziv(),
                p.getOpis(),
                predmetRepo.countByObrazovniProfil_Id(p.getId())
        );
    }

    private MasterPredmetResponse toResponse(MasterPredmet p) {
        return new MasterPredmetResponse(
                p.getId(),
                p.getObrazovniProfil().getId(),
                p.getRazred(),
                p.getNaziv(),
                p.getFondTeorija(),
                p.getFondVezbe(),
                p.getFondBlok(),
                p.getObavezan(),
                p.getRedosled()
        );
    }
}
