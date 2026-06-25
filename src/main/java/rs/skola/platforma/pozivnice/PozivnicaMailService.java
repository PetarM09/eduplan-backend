package rs.skola.platforma.pozivnice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.tenant.domain.Skola;

@Slf4j
@Service
public class PozivnicaMailService {

  private final JavaMailSender mailSender;
  private final String fromAddress;
  private final String fromName;
  private final String replyTo;
  private final String frontendUrl;

  public PozivnicaMailService(
      JavaMailSender mailSender,
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
    this.frontendUrl = frontendUrl.endsWith("/")
        ? frontendUrl.substring(0, frontendUrl.length() - 1)
        : frontendUrl;
  }

  public void posaljiPozivnicu(Korisnik k, Skola skola, UUID token) {
    String link = frontendUrl + "/pozivnica/" + token;
    String skolaNaziv = skola == null ? "" : skola.getNaziv();
    String subject = "Pozivnica za BehindClasses" + (skolaNaziv.isBlank() ? "" : " — " + skolaNaziv);
    try {
      MimeMessage msg = mailSender.createMimeMessage();
      // multipart=true: saljemo i plain-text i HTML alternativu istog maila
      MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
      postaviFrom(helper);
      if (replyTo != null)
        helper.setReplyTo(replyTo);
      helper.setTo(k.getEmail());
      helper.setSubject(subject);
      helper.setText(tekstBody(k, skolaNaziv, link), htmlBody(k, skolaNaziv, link));
      mailSender.send(msg);
      log.info("Poslata pozivnica korisniku {} na {}", k.getId(), k.getEmail());
    } catch (Exception ex) {
      log.error("Greska pri slanju pozivnice {}: {}", k.getId(), ex.getMessage(), ex);
      throw new RuntimeException("Greska pri slanju pozivnice", ex);
    }
  }

  private String tekstBody(Korisnik k, String skolaNaziv, String link) {
    String skoleDeo = skolaNaziv.isBlank() ? "" : " škole " + skolaNaziv;
    return """
        Poštovani %s,

        pozvani ste da pristupite platformi BehindClasses%s.

        Da biste aktivirali nalog i postavili lozinku, otvorite link:
        %s

        Korisničko ime: %s
        Link važi narednih 30 dana.

        Ako niste očekivali ovaj mail, slobodno ga ignorišite.

        — BehindClasses
        """
        .formatted(k.punoIme(), skoleDeo, link, k.getUsername());
  }

  private String htmlBody(Korisnik k, String skolaNaziv, String link) {
    String skolaTekst = skolaNaziv.isBlank() ? "" : " škole <strong>" + escapeHtml(skolaNaziv) + "</strong>";
    return HTML_TEMPLATE
        .replace("{{ime}}", escapeHtml(k.punoIme()))
        .replace("{{skolaTekst}}", skolaTekst)
        .replace("{{username}}", escapeHtml(k.getUsername()))
        .replace("{{link}}", link);
  }

  private void postaviFrom(MimeMessageHelper helper) throws MessagingException {
    if (fromAddress == null)
      return;
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

  private static String escapeHtml(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static final String HTML_TEMPLATE = """
      <!DOCTYPE html>
      <html lang="sr">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="color-scheme" content="light">
        <meta name="supported-color-schemes" content="light">
        <title>Pozivnica za BehindClasses</title>
      </head>
      <body style="margin:0; padding:0; background-color:#f6f8fc;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f6f8fc;">
          <tr>
            <td align="center" style="padding:32px 16px;">
              <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:600px; max-width:600px; background-color:#ffffff; border:1px solid #e3e8ef; border-radius:16px; overflow:hidden; box-shadow:0 4px 16px rgba(14,36,67,0.06); font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <tr>
                  <td style="background-color:#0e2443; padding:28px 40px;">
                    <span style="color:#ffffff; font-size:22px; font-weight:700; letter-spacing:-0.02em;">Behind<span style="color:#f8c007;">Classes</span></span>
                  </td>
                </tr>
                <tr>
                  <td style="height:4px; line-height:4px; font-size:0; background-color:#f8c007;">&nbsp;</td>
                </tr>
                <tr>
                  <td style="padding:40px;">
                    <h1 style="margin:0 0 16px; color:#0e2443; font-size:24px; font-weight:700; letter-spacing:-0.02em;">Pozvani ste</h1>
                    <p style="margin:0 0 20px; color:#0e2443; font-size:16px; line-height:1.6;">Poštovani <strong>{{ime}}</strong>,</p>
                    <p style="margin:0 0 28px; color:#5b6b82; font-size:15px; line-height:1.6;">pozvani ste da pristupite platformi <strong style="color:#0e2443;">BehindClasses</strong>{{skolaTekst}}. Da biste aktivirali nalog i postavili lozinku, kliknite na dugme ispod.</p>
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f1f5f9; border:1px solid #e3e8ef; border-radius:12px; margin:0 0 28px;">
                      <tr>
                        <td style="padding:16px 20px;">
                          <span style="color:#5b6b82; font-size:13px;">Korisničko ime</span><br>
                          <span style="color:#0e2443; font-size:16px; font-weight:600;">{{username}}</span>
                        </td>
                      </tr>
                    </table>
                    <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 28px;">
                      <tr>
                        <td align="center" style="border-radius:12px; background-color:#0e2443;">
                          <a href="{{link}}" target="_blank" style="display:inline-block; padding:14px 36px; color:#ffffff; font-size:16px; font-weight:600; text-decoration:none; border-radius:12px;">Aktiviraj nalog</a>
                        </td>
                      </tr>
                    </table>
                    <p style="margin:0 0 6px; color:#5b6b82; font-size:13px; line-height:1.6;">Ako dugme ne radi, kopirajte ovaj link u pretraživač:</p>
                    <p style="margin:0 0 28px; word-break:break-all;"><a href="{{link}}" style="color:#244c7d; font-size:13px;">{{link}}</a></p>
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-top:1px solid #e3e8ef;">
                      <tr>
                        <td style="padding-top:20px;">
                          <p style="margin:0; color:#5b6b82; font-size:13px; line-height:1.6;">Link važi narednih <strong style="color:#0e2443;">30 dana</strong>. Ako niste očekivali ovaj mail, slobodno ga ignorišite.</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                <tr>
                  <td style="background-color:#f6f8fc; padding:24px 40px; border-top:1px solid #e3e8ef;">
                    <p style="margin:0; color:#5b6b82; font-size:12px; line-height:1.6;">© BehindClasses · školska platforma</p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>
      </body>
      </html>
      """;
}
