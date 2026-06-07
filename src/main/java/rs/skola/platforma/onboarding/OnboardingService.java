package rs.skola.platforma.onboarding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.master.domain.MasterPredmet;
import rs.skola.platforma.master.domain.ObrazovniProfil;
import rs.skola.platforma.master.domain.TipSkole;
import rs.skola.platforma.master.repo.MasterPredmetRepository;
import rs.skola.platforma.master.repo.ObrazovniProfilRepository;
import rs.skola.platforma.master.repo.TipSkoleRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.onboarding.web.PokreniWizardRequest;
import rs.skola.platforma.onboarding.web.WizardPregledRequest;
import rs.skola.platforma.onboarding.web.WizardPregledResponse;
import rs.skola.platforma.onboarding.web.WizardRezultatResponse;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final short NEDELJA_NASTAVE = 36;

    private final TipSkoleRepository tipRepo;
    private final ObrazovniProfilRepository profilRepo;
    private final MasterPredmetRepository masterPredmetRepo;
    private final PredmetRepository predmetRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final SkolaRepository skolaRepo;

    @Transactional(readOnly = true)
    public WizardPregledResponse pregled(WizardPregledRequest req) {
        UUID skolaId = TenantContext.require();
        TipSkole tip = nadjiTip(req.tipSkoleId());
        List<ObrazovniProfil> profili = ucitajIVerifikujProfile(req.obrazovniProfiliIds(), tip);

        Map<String, MasterPredmet> spojeni = new LinkedHashMap<>();
        List<String> upozorenja = new ArrayList<>();
        for (ObrazovniProfil profil : profili) {
            List<MasterPredmet> sviPredmeti = masterPredmetRepo
                    .findAllByObrazovniProfil_IdOrderByRazredAscRedosledAscNazivAsc(profil.getId());
            for (MasterPredmet mp : sviPredmeti) {
                String kljuc = mp.getRazred() + "|" + mp.getNaziv().toLowerCase();
                MasterPredmet postojeci = spojeni.get(kljuc);
                if (postojeci == null) {
                    spojeni.put(kljuc, mp);
                } else if (!istiFond(postojeci, mp)) {
                    upozorenja.add("Konflikt fonda za \"%s\" (%d. razred) izmedju profila — koristi se prvi: %d+%d+%d"
                            .formatted(mp.getNaziv(), mp.getRazred(),
                                    postojeci.getFondTeorija(), postojeci.getFondVezbe(), postojeci.getFondBlok()));
                }
            }
        }

        List<WizardPregledResponse.PredmetUPregledu> predmeti = spojeni.values().stream()
                .map(mp -> {
                    boolean vec = predmetRepo.existsBySkolaIdAndNazivIgnoreCaseAndRazred(
                            skolaId, mp.getNaziv(), mp.getRazred());
                    return new WizardPregledResponse.PredmetUPregledu(
                            mp.getRazred(), mp.getNaziv(),
                            mp.getFondTeorija(), mp.getFondVezbe(), mp.getFondBlok(), vec);
                })
                .toList();
        return new WizardPregledResponse(predmeti, upozorenja);
    }

    @Transactional
    public WizardRezultatResponse pokreni(PokreniWizardRequest req) {
        UUID skolaId = TenantContext.require();
        Skola skola = skolaRepo.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));

        TipSkole tip = nadjiTip(req.tipSkoleId());
        List<ObrazovniProfil> profili = ucitajIVerifikujProfile(req.obrazovniProfiliIds(), tip);
        validirajRazrede(req.razredi(), tip);

        // 1. Skola pamti svoje profile (replace — ne kumuliramo silently).
        skola.setObrazovniProfiliIds(req.obrazovniProfiliIds());

        // 2. Predmeti: merge po (razred, naziv).
        Map<String, MasterPredmet> spojeni = new LinkedHashMap<>();
        List<String> upozorenja = new ArrayList<>();
        for (ObrazovniProfil p : profili) {
            for (MasterPredmet mp : masterPredmetRepo
                    .findAllByObrazovniProfil_IdOrderByRazredAscRedosledAscNazivAsc(p.getId())) {
                String kljuc = mp.getRazred() + "|" + mp.getNaziv().toLowerCase();
                MasterPredmet postojeci = spojeni.get(kljuc);
                if (postojeci == null) {
                    spojeni.put(kljuc, mp);
                } else if (!istiFond(postojeci, mp)) {
                    upozorenja.add("Konflikt fonda za \"%s\" (%d. razred); koristi se prvi pronadjeni."
                            .formatted(mp.getNaziv(), mp.getRazred()));
                }
            }
        }

        int novihPredmeta = 0, preskocenihPredmeta = 0;
        for (MasterPredmet mp : spojeni.values()) {
            if (predmetRepo.existsBySkolaIdAndNazivIgnoreCaseAndRazred(
                    skolaId, mp.getNaziv(), mp.getRazred())) {
                preskocenihPredmeta++;
                continue;
            }
            Predmet noviPredmet = Predmet.builder()
                    .naziv(mp.getNaziv())
                    .razred(mp.getRazred())
                    .fondTeorija(mp.getFondTeorija())
                    .fondVezbe(mp.getFondVezbe())
                    .fondBlok(mp.getFondBlok())
                    .fondCasova((short) ((mp.getFondTeorija() + mp.getFondVezbe() + mp.getFondBlok()) * NEDELJA_NASTAVE))
                    .masterPredmetId(mp.getId())
                    .aktivan(true)
                    .build();
            noviPredmet.setSkolaId(skolaId);
            predmetRepo.save(noviPredmet);
            novihPredmeta++;
        }

        // 3. Odeljenja: kreiraj po razredima/oznakama, preskoci postojeca.
        int novihOdeljenja = 0, preskocenihOdeljenja = 0;
        for (PokreniWizardRequest.RazredKonfig rk : req.razredi()) {
            for (String oznaka : rk.oznake()) {
                String trim = oznaka == null ? "" : oznaka.trim();
                if (trim.isEmpty()) continue;
                if (odeljenjeRepo.findBySkolaIdAndRazredAndOznakaAndSkolskaGodina(
                        skolaId, rk.razred(), trim, req.skolskaGodina()).isPresent()) {
                    preskocenihOdeljenja++;
                    continue;
                }
                Odeljenje o = Odeljenje.builder()
                        .razred(rk.razred())
                        .oznaka(trim)
                        .skolskaGodina(req.skolskaGodina())
                        .aktivan(true)
                        .build();
                o.setSkolaId(skolaId);
                odeljenjeRepo.save(o);
                novihOdeljenja++;
            }
        }

        log.info("Wizard za skolu {}: +{} predmeta (preskoceno {}), +{} odeljenja (preskoceno {})",
                skolaId, novihPredmeta, preskocenihPredmeta, novihOdeljenja, preskocenihOdeljenja);
        return new WizardRezultatResponse(novihPredmeta, preskocenihPredmeta,
                novihOdeljenja, preskocenihOdeljenja, upozorenja);
    }

    // -------- helpers --------

    private TipSkole nadjiTip(UUID id) {
        return tipRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tip skole", id));
    }

    private List<ObrazovniProfil> ucitajIVerifikujProfile(List<UUID> ids, TipSkole tip) {
        List<ObrazovniProfil> profili = profilRepo.findAllById(ids);
        if (profili.size() != ids.size()) {
            throw new ResourceNotFoundException("Neki obrazovni profil ne postoji u katalogu");
        }
        for (ObrazovniProfil p : profili) {
            if (!p.getTipSkole().getId().equals(tip.getId())) {
                throw new TenantViolationException(
                        "Profil \"" + p.getNaziv() + "\" ne pripada tipu " + tip.getNaziv());
            }
        }
        return profili;
    }

    private void validirajRazrede(List<PokreniWizardRequest.RazredKonfig> rk, TipSkole tip) {
        for (PokreniWizardRequest.RazredKonfig r : rk) {
            if (r.razred() < 1 || r.razred() > tip.getUkupnoRazreda()) {
                throw new ValidationException("Razred " + r.razred() + " nije validan za " + tip.getNaziv()
                        + " (1-" + tip.getUkupnoRazreda() + ")");
            }
        }
    }

    private boolean istiFond(MasterPredmet a, MasterPredmet b) {
        return a.getFondTeorija().equals(b.getFondTeorija())
                && a.getFondVezbe().equals(b.getFondVezbe())
                && a.getFondBlok().equals(b.getFondBlok());
    }
}
