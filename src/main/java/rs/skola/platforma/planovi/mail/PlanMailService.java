package rs.skola.platforma.planovi.mail;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.OperativniPlan;
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

    public void posaljiOperativniPlan(OperativniPlan plan, Skola skola, byte[] wordBytes, String primalac) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(primalac);
            helper.setSubject("Operativni plan — " + plan.getNastavnik().punoIme()
                    + " — " + plan.getPredmet().getNaziv()
                    + " — " + plan.getOdeljenje().label()
                    + " (" + nazivMeseca(plan.getMesec()) + " " + plan.getSkolskaGodina() + ")");
            helper.setText(teloOperativnog(plan, skola), false);

            String fileName = "OperativniPlan_%s_%s_%s_%s.docx".formatted(
                    plan.getPredmet().getNaziv().replaceAll("\\s+", "_"),
                    plan.getOdeljenje().label(),
                    nazivMeseca(plan.getMesec()),
                    plan.getSkolskaGodina().replace("/", "-"));
            helper.addAttachment(fileName, new ByteArrayDataSource(wordBytes,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

            mailSender.send(msg);
            log.info("Poslat operativni plan {} na {}", plan.getId(), primalac);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a za operativni plan {}: {}", plan.getId(), ex.getMessage(), ex);
        }
    }

    private String teloOperativnog(OperativniPlan plan, Skola skola) {
        return """
                Postovani,

                u prilogu se nalazi operativni plan rada:
                  Nastavnik:        %s
                  Predmet:          %s
                  Odeljenje:        %s
                  Mesec:            %s
                  Skolska godina:   %s
                %s
                Plan je generisan automatski iz skolske platforme.
                """.formatted(
                plan.getNastavnik().punoIme(),
                plan.getPredmet().getNaziv(),
                plan.getOdeljenje().label(),
                nazivMeseca(plan.getMesec()),
                plan.getSkolskaGodina(),
                skola == null ? "" : "  Skola:            " + skola.getNaziv() + "\n");
    }

    private static String nazivMeseca(Short m) {
        return switch (m == null ? 0 : m.intValue()) {
            case 1 -> "Januar"; case 2 -> "Februar"; case 3 -> "Mart"; case 4 -> "April";
            case 5 -> "Maj"; case 6 -> "Jun"; case 7 -> "Jul"; case 8 -> "Avgust";
            case 9 -> "Septembar"; case 10 -> "Oktobar"; case 11 -> "Novembar"; case 12 -> "Decembar";
            default -> "?";
        };
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
