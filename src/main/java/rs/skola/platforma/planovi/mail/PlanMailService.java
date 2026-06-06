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

@Slf4j
@Service
public class PlanMailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;

    public PlanMailService(JavaMailSender mailSender,
                            @Value("${app.mail.from:}") String mailFrom,
                            @Value("${app.mail.reply-to:}") String mailReplyTo) {
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
        this.replyTo = (mailReplyTo == null || mailReplyTo.isBlank()) ? null : mailReplyTo.trim();
    }

    public void posaljiGodisnjiPlan(GodisnjiPlan plan, Skola skola, byte[] wordBytes, byte[] pdfBytes, String primalac) {
        String subject = "Godisnji plan rada — " + plan.getNastavnik().punoIme()
                + " — " + plan.getPredmet().getNaziv() + " (" + plan.getSkolskaGodina() + ")";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            postaviFrom(helper);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setTo(primalac);
            helper.setSubject(subject);
            helper.setText(telo(plan, skola), false);

            String baseName = "GodisnjiPlan_%s_%s".formatted(
                    plan.getPredmet().getNaziv().replaceAll("\\s+", "_"),
                    plan.getSkolskaGodina().replace("/", "-"));
            if (wordBytes != null) {
                helper.addAttachment(baseName + ".docx", new ByteArrayDataSource(wordBytes,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            }
            if (pdfBytes != null) {
                helper.addAttachment(baseName + ".pdf", new ByteArrayDataSource(pdfBytes, "application/pdf"));
            }

            mailSender.send(msg);
            log.info("Poslat godisnji plan {} | From={} | To={} | Reply-To={} | Subject={}",
                    plan.getId(), opisiFrom(), primalac, replyTo == null ? "-" : replyTo, subject);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a za plan {} (primalac={}): {}",
                    plan.getId(), primalac, ex.getMessage(), ex);
        }
    }

    public void posaljiOdbijanjeGodisnjeg(GodisnjiPlan plan, Skola skola, String razlog, String primalac) {
        String subject = "Godisnji plan vracen na doradu — " + plan.getPredmet().getNaziv()
                + " (" + plan.getSkolskaGodina() + ")";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            postaviFrom(helper);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setTo(primalac);
            helper.setSubject(subject);
            helper.setText(teloOdbijanjaGodisnjeg(plan, skola, razlog), false);
            mailSender.send(msg);
            log.info("Poslat mail o odbijanju godisnjeg plana {} primaocu {}", plan.getId(), primalac);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a o odbijanju godisnjeg plana {}: {}",
                    plan.getId(), ex.getMessage(), ex);
        }
    }

    public void posaljiOdbijanjeOperativnog(OperativniPlan plan, Skola skola, String razlog, String primalac) {
        String subject = "Operativni plan vracen na doradu — " + plan.getPredmet().getNaziv()
                + " — " + plan.getOdeljenje().label()
                + " (" + nazivMeseca(plan.getMesec()) + " " + plan.getSkolskaGodina() + ")";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            postaviFrom(helper);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setTo(primalac);
            helper.setSubject(subject);
            helper.setText(teloOdbijanjaOperativnog(plan, skola, razlog), false);
            mailSender.send(msg);
            log.info("Poslat mail o odbijanju operativnog plana {} primaocu {}", plan.getId(), primalac);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a o odbijanju operativnog plana {}: {}",
                    plan.getId(), ex.getMessage(), ex);
        }
    }

    private String teloOdbijanjaGodisnjeg(GodisnjiPlan plan, Skola skola, String razlog) {
        return """
                Postovani,

                Vas godisnji plan rada je vracen na doradu od strane PP sluzbe:
                  Predmet:          %s
                  Razred:           %s
                  Skolska godina:   %s
                %s
                Razlog vracanja:
                %s

                Molimo Vas da uradite potrebne izmene i ponovo podnesete plan kroz skolsku platformu.
                """.formatted(
                plan.getPredmet().getNaziv(),
                plan.getRazred() == null ? "-" : plan.getRazred(),
                plan.getSkolskaGodina(),
                skola == null ? "" : "  Skola:            " + skola.getNaziv() + "\n",
                razlog == null || razlog.isBlank() ? "(nije naveden)" : razlog);
    }

    private String teloOdbijanjaOperativnog(OperativniPlan plan, Skola skola, String razlog) {
        return """
                Postovani,

                Vas operativni plan rada je vracen na doradu od strane PP sluzbe:
                  Predmet:          %s
                  Odeljenje:        %s
                  Mesec:            %s
                  Skolska godina:   %s
                %s
                Razlog vracanja:
                %s

                Molimo Vas da uradite potrebne izmene i ponovo podnesete plan kroz skolsku platformu.
                """.formatted(
                plan.getPredmet().getNaziv(),
                plan.getOdeljenje().label(),
                nazivMeseca(plan.getMesec()),
                plan.getSkolskaGodina(),
                skola == null ? "" : "  Skola:            " + skola.getNaziv() + "\n",
                razlog == null || razlog.isBlank() ? "(nije naveden)" : razlog);
    }

    public void posaljiOperativniPlan(OperativniPlan plan, Skola skola, byte[] wordBytes, byte[] pdfBytes, String primalac) {
        String subject = "Operativni plan — " + plan.getNastavnik().punoIme()
                + " — " + plan.getPredmet().getNaziv()
                + " — " + plan.getOdeljenje().label()
                + " (" + nazivMeseca(plan.getMesec()) + " " + plan.getSkolskaGodina() + ")";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            postaviFrom(helper);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setTo(primalac);
            helper.setSubject(subject);
            helper.setText(teloOperativnog(plan, skola), false);

            String baseName = "OperativniPlan_%s_%s_%s_%s".formatted(
                    plan.getPredmet().getNaziv().replaceAll("\\s+", "_"),
                    plan.getOdeljenje().label(),
                    nazivMeseca(plan.getMesec()),
                    plan.getSkolskaGodina().replace("/", "-"));
            if (wordBytes != null) {
                helper.addAttachment(baseName + ".docx", new ByteArrayDataSource(wordBytes,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            }
            if (pdfBytes != null) {
                helper.addAttachment(baseName + ".pdf", new ByteArrayDataSource(pdfBytes, "application/pdf"));
            }

            mailSender.send(msg);
            log.info("Poslat operativni plan {} | From={} | To={} | Reply-To={} | Subject={}",
                    plan.getId(), opisiFrom(), primalac, replyTo == null ? "-" : replyTo, subject);
        } catch (Exception ex) {
            log.error("Greska pri slanju mail-a za operativni plan {} (primalac={}): {}",
                    plan.getId(), primalac, ex.getMessage(), ex);
        }
    }

    private String opisiFrom() {
        if (fromAddress == null) return "(default mail.username)";
        return fromName == null ? fromAddress : fromName + " <" + fromAddress + ">";
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
