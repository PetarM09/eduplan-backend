package rs.skola.platforma.planovi.mail;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.tenant.domain.Skola;

/**
 * Šalje generisani Word plan na mail adresu skole konfigurisanu kroz
 * {@code skole.mail_planovi}. Poziv je sinhroni — caller (PlanIsporukaService) je
 * vec u {@code @Async} kontekstu pa nemamo dvostruko nesting-anje.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanMailService {

    private final JavaMailSender mailSender;

    public void posaljiGodisnjiPlan(GodisnjiPlan plan, Skola skola, byte[] wordBytes, String primalac) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(primalac);
            helper.setSubject("Godisnji plan rada — " + plan.getNastavnik().punoIme()
                    + " — " + plan.getPredmet().getNaziv() + " (" + plan.getSkolskaGodina() + ")");
            helper.setText(telo(plan, skola), false);

            String fileName = "GodisnjiPlan_%s_%s.docx".formatted(
                    plan.getPredmet().getNaziv().replaceAll("\\s+", "_"),
                    plan.getSkolskaGodina().replace("/", "-"));
            helper.addAttachment(fileName, new ByteArrayDataSource(wordBytes,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

            mailSender.send(msg);
            log.info("Poslat godisnji plan {} na {}", plan.getId(), primalac);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a za plan {}: {}", plan.getId(), ex.getMessage(), ex);
        }
    }

    private String telo(GodisnjiPlan plan, Skola skola) {
        return """
                Postovani,

                u prilogu se nalazi godisnji plan rada:
                  Nastavnik:        %s
                  Predmet:          %s
                  Razred:           %s
                  Skolska godina:   %s
                %s
                Plan je generisan automatski iz skolske platforme.
                """.formatted(
                plan.getNastavnik().punoIme(),
                plan.getPredmet().getNaziv(),
                plan.getRazred() == null ? "-" : plan.getRazred(),
                plan.getSkolskaGodina(),
                skola == null ? "" : "  Skola:            " + skola.getNaziv() + "\n");
    }
}
