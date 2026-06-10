package rs.skola.platforma.planovi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanObradaService {

    private final GodisnjiPlanRepository planRepo;
    private final SkolaRepository skolaRepo;
    private final PlanExportService exportService;
    private final PlanStorageService storageService;
    private final PlanMailService mailService;

    @Transactional
    public void obradi(UUID planId) {
        GodisnjiPlan plan = planRepo.findByIdSaTemama(planId).orElse(null);
        if (plan == null) {
            log.warn("Plan {} ne postoji — preskacem generisanje", planId);
            return;
        }
        TenantContext.set(plan.getSkolaId());
        try {
            byte[] wordBytes = exportService.generisiGodisnjiPlanWord(plan);
            String wordPath = storageService.sacuvajWord(plan.getSkolaId(), plan.getId(), wordBytes);
            plan.setWordFajlPutanja(wordPath);

            // PDF konverzija zahteva LibreOffice — ako nije instaliran ili
            // padne, ne smemo da prekinemo cuvanje Word-a. Plan ostaje upotrebljiv.
            try {
                byte[] pdfBytes = exportService.generisiGodisnjiPlanPdf(plan);
                String pdfPath = storageService.sacuvajPdf(plan.getSkolaId(), plan.getId(), pdfBytes);
                plan.setPdfFajlPutanja(pdfPath);
            } catch (Exception ex) {
                log.warn("PDF nije generisan za plan {} ({}). Word je sacuvan.",
                        planId, ex.getMessage());
            }
            planRepo.save(plan);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void posaljiNaSkolskiMail(UUID planId) {
        GodisnjiPlan plan = planRepo.findByIdSaTemama(planId).orElse(null);
        if (plan == null) {
            log.warn("Plan {} ne postoji — preskacem slanje na skolski mail", planId);
            return;
        }
        TenantContext.set(plan.getSkolaId());
        try {
            Skola skola = skolaRepo.findById(plan.getSkolaId()).orElse(null);
            String adresa = skola == null ? null : skola.getMailPlanovi();
            if (adresa == null || adresa.isBlank()) {
                log.info("Skola nema podesen mail za planove — preskacem slanje plana {}", plan.getId());
                return;
            }
            byte[] wordBytes;
            byte[] pdfBytes = null;
            if (plan.getWordFajlPutanja() == null) {
                wordBytes = exportService.generisiGodisnjiPlanWord(plan);
                String wordPath = storageService.sacuvajWord(plan.getSkolaId(), plan.getId(), wordBytes);
                plan.setWordFajlPutanja(wordPath);
            } else {
                wordBytes = storageService.procitaj(plan.getSkolaId(), plan.getId(), plan.getWordFajlPutanja());
            }
            if (plan.getPdfFajlPutanja() != null) {
                pdfBytes = storageService.procitaj(plan.getSkolaId(), plan.getId(), plan.getPdfFajlPutanja());
            } else {
                try {
                    pdfBytes = exportService.generisiGodisnjiPlanPdf(plan);
                    String pdfPath = storageService.sacuvajPdf(plan.getSkolaId(), plan.getId(), pdfBytes);
                    plan.setPdfFajlPutanja(pdfPath);
                } catch (Exception ex) {
                    log.warn("PDF nije generisan pri slanju plana {} ({}). Saljemo samo Word.",
                            planId, ex.getMessage());
                }
            }
            planRepo.save(plan);
            mailService.posaljiGodisnjiPlan(plan, skola, wordBytes, pdfBytes, adresa);
        } finally {
            TenantContext.clear();
        }
    }
}
