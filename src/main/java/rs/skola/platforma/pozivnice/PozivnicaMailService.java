package rs.skola.platforma.pozivnice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.tenant.domain.Skola;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Slf4j
@Service
public class PozivnicaMailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;
    private final String frontendUrl;

    public PozivnicaMailService(JavaMailSender mailSender,
                                  @Value("${app.mail.from:}") String mailFrom,
                                  @Value("${app.mail.reply-to:}") String mailReplyTo,
                                  @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
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
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public void posaljiPozivnicu(Korisnik k, Skola skola, UUID token) {
        String link = frontendUrl + "/pozivnica/" + token;
        String subject = "Pozivnica za skolsku platformu — " + (skola == null ? "" : skola.getNaziv());
        String body = """
                Postovani %s,

                pozvani ste da pristupite skolskoj platformi%s.

                Da biste aktivirali nalog i postavili sifru, kliknite na link:
                %s

                Korisnicko ime: %s
                Link je vazi narednih 30 dana.

                Ako niste ocekivali ovaj mail, slobodno ga ignorisite.
                """.formatted(
                k.punoIme(),
                skola == null ? "" : " skole " + skola.getNaziv(),
                link,
                k.getUsername());
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            postaviFrom(helper);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setTo(k.getEmail());
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(msg);
            log.info("Poslata pozivnica korisniku {} na {}", k.getId(), k.getEmail());
        } catch (Exception ex) {
            log.error("Greska pri slanju pozivnice {}: {}", k.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Greska pri slanju pozivnice", ex);
        }
    }

    private void postaviFrom(MimeMessageHelper helper) throws MessagingException {
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
}
