package rs.skola.platforma.planovi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.planovi.export.PlanExportService;
import rs.skola.platforma.planovi.export.PlanStorageService;
import rs.skola.platforma.planovi.mail.PlanMailService;
import rs.skola.platforma.planovi.repo.OperativniPlanRepository;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.util.UUID;

/**
 * Transakciona "obradi" logika izvojena u poseban bean da bi {@code @Async} u
 * {@link OperativniPlanIsporukaService} mogao da je pozove preko Spring proxy-ja,
 * cime se {@code @Transactional} stvarno aktivira (interni self-poziv ne ide kroz proxy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperativniPlanObradaService {

    private final OperativniPlanRepository planRepo;
    private final SkolaRepository skolaRepo;
    private final PlanExportService exportService;
    private final PlanStorageService storageService;
    private final PlanMailService mailService;

    @Transactional
    public void obradi(UUID planId) {
        OperativniPlan plan = planRepo.findByIdSaStavkama(planId).orElse(null);
        if (plan == null) {
            log.warn("Operativni plan {} ne postoji — preskacem isporuku", planId);
            return;
        }
        // "Cannot fetch multiple bags" u JPQL: lazy kolekcije inicijalizujemo
        // eksplicitno unutar transakcije pre prelaska na eksport.
        plan.getStavke().forEach(s -> {
            s.getIshodi().size();
            s.getMedjupredmetno().forEach(mp -> mp.getPredmet().getNaziv());
        });
        TenantContext.set(plan.getSkolaId());
        try {
            byte[] wordBytes = exportService.generisiOperativniPlanWord(plan);
            byte[] pdfBytes = exportService.generisiOperativniPlanPdf(plan);

            String wordPath = storageService.sacuvajOperativniWord(plan.getSkolaId(), plan.getId(), wordBytes);
            String pdfPath = storageService.sacuvajOperativniPdf(plan.getSkolaId(), plan.getId(), pdfBytes);
            plan.setWordFajlPutanja(wordPath);
            plan.setPdfFajlPutanja(pdfPath);
            planRepo.save(plan);

            Skola skola = skolaRepo.findById(plan.getSkolaId()).orElse(null);
            String adresa = skola == null ? null : skola.getMailPlanovi();
            if (adresa != null && !adresa.isBlank()) {
                mailService.posaljiOperativniPlan(plan, skola, wordBytes, adresa);
            } else {
                log.info("Skola nema mail za planove — preskacem slanje operativnog plana {}", plan.getId());
            }
        } finally {
            TenantContext.clear();
        }
    }
}
