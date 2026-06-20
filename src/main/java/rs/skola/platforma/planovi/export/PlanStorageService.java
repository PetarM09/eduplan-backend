package rs.skola.platforma.planovi.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;

import java.util.UUID;

/**
 * Cuva generisane planove (.docx/.pdf) kao blobove u bazi (tabela plan_fajl).
 * Disk se ne koristi jer je na Heroku efemeran. Metode vracaju logicko ime
 * fajla koje pozivaoci upisuju u *_fajl_putanja kao marker da fajl postoji;
 * to ime se pri citanju koristi samo da se razlikuje WORD od PDF-a.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanStorageService {

    private final PlanFajlRepository repo;

    public String sacuvajWord(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, PlanTip.GODISNJI, planId, FajlTip.WORD, bytes, "godisnji-plan.docx");
    }

    public String sacuvajPdf(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, PlanTip.GODISNJI, planId, FajlTip.PDF, bytes, "godisnji-plan.pdf");
    }

    public String sacuvajOperativniWord(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, PlanTip.OPERATIVNI, planId, FajlTip.WORD, bytes, "operativni-plan.docx");
    }

    public String sacuvajOperativniPdf(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, PlanTip.OPERATIVNI, planId, FajlTip.PDF, bytes, "operativni-plan.pdf");
    }

    private String sacuvaj(UUID skolaId, PlanTip planTip, UUID planId, FajlTip fajlTip,
                           byte[] bytes, String ime) {
        PlanFajl fajl = repo.findByPlanTipAndPlanIdAndFajlTip(planTip, planId, fajlTip)
                .orElseGet(PlanFajl::new);
        fajl.setSkolaId(skolaId);
        fajl.setPlanTip(planTip);
        fajl.setPlanId(planId);
        fajl.setFajlTip(fajlTip);
        fajl.setSadrzaj(bytes);
        repo.save(fajl);
        return ime;
    }

    public byte[] procitaj(UUID skolaId, UUID planId, String marker) {
        return procitaj(skolaId, PlanTip.GODISNJI, planId, marker);
    }

    public byte[] procitajOperativni(UUID skolaId, UUID planId, String marker) {
        return procitaj(skolaId, PlanTip.OPERATIVNI, planId, marker);
    }

    private byte[] procitaj(UUID skolaId, PlanTip planTip, UUID planId, String marker) {
        if (marker == null) {
            throw new ResourceNotFoundException("Fajl nije generisan");
        }
        FajlTip fajlTip = marker.endsWith(".pdf") ? FajlTip.PDF : FajlTip.WORD;
        PlanFajl fajl = repo.findByPlanTipAndPlanIdAndFajlTip(planTip, planId, fajlTip)
                .orElseThrow(() -> new ResourceNotFoundException("Fajl ne postoji: " + marker));
        if (!fajl.getSkolaId().equals(skolaId)) {
            log.warn("Pokusaj pristupa fajlu druge skole: plan {} (skola {})", planId, skolaId);
            throw new TenantViolationException("Pristup fajlu nije dozvoljen");
        }
        return fajl.getSadrzaj();
    }
}
