package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super("KONFLIKT", HttpStatus.CONFLICT, message);
    }

    public ConflictException(String code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }
}
