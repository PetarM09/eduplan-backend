package rs.skola.platforma.planovi.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.tenant.domain.Skola;

import java.io.UnsupportedEncodingException;

/**
 * Šalje generisani Word plan na mail adresu skole konfigurisanu kroz
 * {@code skole.mail_planovi}. Poziv je sinhroni — caller (PlanIsporukaService) je
 * vec u {@code @Async} kontekstu pa nemamo dvostruko nesting-anje.
 *
 * <p>From adresa se cita iz {@code app.mail.from} (env {@code MAIL_FROM}).
 * Format moze biti "Ime <adresa>" ili samo "adresa". Ako prop nije postavljen,
 * Spring pada na sistemski default (obicno mail.username sa servera).
 */
@Slf4j
@Service
public class PlanMailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public PlanMailService(JavaMailSender mailSender,
                            @Value("${app.mail.from:}") String mailFrom) {
        this.mailSender = mailSender;
        // Razdvajamo "Ime <adresa>" na ime i adresu — Spring zahteva to za setFrom(adresa, ime)
        String trimmed = mailFrom == null ? "" : mailFrom.trim();
        if (trimmed.contains("<") && trimmed.endsWith(">")) {
            int ltIdx = trimmed.indexOf('<');
            this.fromName = trimmed.substring(0, ltIdx).trim();
            this.fromAddress = trimmed.substring(ltIdx + 1, trimmed.length() - 1).trim();
        } else {
            this.fromName = null;
            this.fromAddress = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public void posaljiGodisnjiPlan(GodisnjiPlan plan, Skola skola, byte[] wordBytes, String primalac) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            postaviFrom(helper);
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
            postaviFrom(helper);
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

    /** Postavlja From header iz konfiguracije ako je dostupno. */
    private void postaviFrom(MimeMessageHelper helper) throws jakarta.mail.MessagingException {
        if (fromAddress == null) return;
        try {
            if (fromName != null && !fromName.isEmpty()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
            } else {
                helper.setFrom(fromAddress);
            }
        } catch (UnsupportedEncodingException ex) {
            helper.setFrom(fromAddress);
        }
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
