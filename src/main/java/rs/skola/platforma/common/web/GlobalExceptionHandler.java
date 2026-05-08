package rs.skola.platforma.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import rs.skola.platforma.common.exception.BaseException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBase(BaseException ex, HttpServletRequest req) {
        log.warn("Domenska greska na {} {}: {} - {}", req.getMethod(), req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> polja = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("polje", fe.getField(), "poruka", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDACIONA_GRESKA", "Greske u zahtevu", polja));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, String>> polja = ex.getConstraintViolations().stream()
                .map(this::toPolje)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDACIONA_GRESKA", "Greske u parametrima", polja));
    }

    private Map<String, String> toPolje(ConstraintViolation<?> cv) {
        return Map.of("polje", cv.getPropertyPath().toString(), "poruka", cv.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleParam(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("LOSI_PARAMETRI", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("NEISPRAVNI_KREDENCIJALI", "Pogresno korisnicko ime ili lozinka"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("NEAUTORIZOVANO", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("PRISTUP_ZABRANJEN", "Nemate dozvolu za ovu akciju"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METODA_NIJE_PODRZANA", ex.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RUTA_NIJE_NADJENA", "Endpoint ne postoji"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Neocekivana greska na {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNA_GRESKA", "Doslo je do greske na serveru"));
    }
}
