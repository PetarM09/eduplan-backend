package rs.skola.platforma.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * Uniformni response wrapper za sve uspesne i neuspesne odgovore.
 * Greske su u {@link ErrorPayload}, dok success vraca {@code data} i prazan error.
 */
public record ApiResponse<T>(
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL) T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) ErrorPayload error,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message, null), OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message, details), OffsetDateTime.now());
    }

    public record ErrorPayload(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL) Object details
    ) {}
}
