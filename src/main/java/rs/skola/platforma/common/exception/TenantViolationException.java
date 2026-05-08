package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Baca se kada korisnik pokusa da pristupi resursu izvan svoje skole.
 * Rezultat je 403 Forbidden — ne 404 — jer pristup postoji ali je zabranjen.
 */
public class TenantViolationException extends BaseException {

    public TenantViolationException() {
        super("PRISTUP_ZABRANJEN", HttpStatus.FORBIDDEN,
                "Resurs pripada drugoj skoli ili nemate dozvolu");
    }

    public TenantViolationException(String message) {
        super("PRISTUP_ZABRANJEN", HttpStatus.FORBIDDEN, message);
    }
}
