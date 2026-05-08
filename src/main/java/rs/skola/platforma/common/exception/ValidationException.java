package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super("VALIDACIONA_GRESKA", HttpStatus.BAD_REQUEST, message);
    }

    public ValidationException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}
