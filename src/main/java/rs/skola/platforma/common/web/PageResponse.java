package rs.skola.platforma.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Lagani DTO za pagininane odgovore — izbegava da se Spring-ov Page interni
 * format ({@code pageable}, {@code sort.unsorted} itd.) propagira do klijenta.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
