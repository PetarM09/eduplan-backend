package rs.skola.platforma.planovi.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.OperativniPlan;

/**
 * Tanki adapter koji zadrzava postojece pozive iz `PlanObradaService` i
 * `OperativniPlanObradaService`. Realna logika je u `PlanTemplateService`
 * (popunjavanje .docx sablona) i `WordToPdfConverter` (Word -> PDF preko
 * LibreOffice headless).
 */
@Service
@RequiredArgsConstructor
public class PlanExportService {

    private final PlanTemplateService templateService;

    public byte[] generisiGodisnjiPlanWord(GodisnjiPlan plan) {
        return templateService.godisnjiWord(plan);
    }

    public byte[] generisiGodisnjiPlanPdf(GodisnjiPlan plan) {
        return templateService.godisnjiPdf(plan);
    }

    public byte[] generisiOperativniPlanWord(OperativniPlan plan) {
        return templateService.operativniWord(plan);
    }

    public byte[] generisiOperativniPlanPdf(OperativniPlan plan) {
        return templateService.operativniPdf(plan);
    }
}
