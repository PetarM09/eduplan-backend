package rs.skola.platforma.planovi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.export.PlanExportService;
import rs.skola.platforma.planovi.export.PlanStorageService;
import rs.skola.platforma.planovi.mail.PlanMailService;
import rs.skola.platforma.planovi.repo.GodisnjiPlanRepository;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.util.UUID;

/**
 * Orkestrira sve sto se desava nakon cuvanja plana: generisanje Word i PDF dokumenta,
 * upis fajlova u storage i slanje na mail skole. Asinhrono — nastavnik ne ceka.
 *
 * <p>Bilo koja greska se logira i ne propagira nazad ka korisniku, jer je glavna
 * transakcija (upis plana u bazu) vec uspesno commitovana pre poziva ovog servisa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanIsporukaService {

    private final GodisnjiPlanRepository planRepo;
    private final SkolaRepository skolaRepo;
    private final PlanExportService exportService;
    private final PlanStorageService storageService;
    private final PlanMailService mailService;

    /**
     * Snima skola_id pre nego sto se thread prebaci u async pool — ThreadLocal
     * TenantContext se ne nasledjuje automatski.
     */
    @Async
    public void isporuciAsinhrono(UUID planId) {
        try {
            obradi(planId);
        } catch (Exception ex) {
            log.error("Greska pri asinhronoj isporuci plana {}: {}", planId, ex.getMessage(), ex);
        }
    }

    @Transactional
    public void obradi(UUID planId) {
        GodisnjiPlan plan = planRepo.findByIdSaTemama(planId).orElse(null);
        if (plan == null) {
            log.warn("Plan {} ne postoji — preskacem isporuku", planId);
            return;
        }
        TenantContext.set(plan.getSkolaId());
        try {
            byte[] wordBytes = exportService.generisiGodisnjiPlanWord(plan);
            byte[] pdfBytes = exportService.generisiGodisnjiPlanPdf(plan);

            String wordPath = storageService.sacuvajWord(plan.getSkolaId(), plan.getId(), wordBytes);
            String pdfPath = storageService.sacuvajPdf(plan.getSkolaId(), plan.getId(), pdfBytes);

            plan.setWordFajlPutanja(wordPath);
            plan.setPdfFajlPutanja(pdfPath);
            planRepo.save(plan);

            Skola skola = skolaRepo.findById(plan.getSkolaId()).orElse(null);
            String mailAdresa = skola == null ? null : skola.getMailPlanovi();
            if (mailAdresa != null && !mailAdresa.isBlank()) {
                mailService.posaljiGodisnjiPlan(plan, skola, wordBytes, mailAdresa);
            } else {
                log.info("Skola nema podesen mail za planove — preskacem slanje plana {}", plan.getId());
            }
        } finally {
            TenantContext.clear();
        }
    }
}
