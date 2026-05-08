package rs.skola.platforma.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resurs, Object id) {
        super("RESURS_NIJE_NADJEN", HttpStatus.NOT_FOUND,
                "%s sa identifikatorom %s nije pronadjen".formatted(resurs, id));
    }

    public ResourceNotFoundException(String message) {
        super("RESURS_NIJE_NADJEN", HttpStatus.NOT_FOUND, message);
    }
}
