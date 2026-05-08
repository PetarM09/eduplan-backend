package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super("NEAUTORIZOVANO", HttpStatus.UNAUTHORIZED, message);
    }
}
