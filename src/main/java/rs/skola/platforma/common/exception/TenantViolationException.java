package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

public class TenantViolationException extends BaseException {

    public TenantViolationException() {
        super("PRISTUP_ZABRANJEN", HttpStatus.FORBIDDEN,
                "Resurs pripada drugoj skoli ili nemate dozvolu");
    }

    public TenantViolationException(String message) {
        super("PRISTUP_ZABRANJEN", HttpStatus.FORBIDDEN, message);
    }
}
