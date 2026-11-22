package co.edu.unicesar.sierap.service.impl;

import java.text.Normalizer;
import java.util.Locale;

import co.edu.unicesar.sierap.dto.response.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/**
 * Utilidades de paginación y búsqueda de los services (ADR-024).
 *
 * <p>La paginación es opcional: sin {@code page} ni {@code size} el controller pasa un
 * {@link Pageable#unpaged()} y la lista sale completa, igual que antes.</p>
 */
final class Paging {

    /** Carácter de escape de los comodines de {@code LIKE} en las consultas de búsqueda. */
    static final char LIKE_ESCAPE = '!';

    private Paging() {
    }

    /**
     * Aplica el orden de la lista a la paginación pedida, conservando si hay página o no.
     *
     * @param pageable paginación recibida (puede ser sin paginar)
     * @param sort     orden propio de la lista
     * @return la misma página o el mismo "sin paginar", ordenados con {@code sort}
     */
    static Pageable sorted(Pageable pageable, Sort sort) {
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }

    /**
     * Convierte una página de entidades en el resultado de la API.
     *
     * @param page   página de entidades
     * @param mapper conversión de cada entidad a su DTO
     * @param <E>    tipo de la entidad
     * @param <T>    tipo del DTO
     * @return elementos de la página y total
     */
    static <E, T> PagedResult<T> toResult(Page<E> page, Function<E, T> mapper) {
        List<T> items = page.getContent().stream().map(mapper).toList();
        return new PagedResult<>(items, page.getTotalElements());
    }

    /**
     * Prepara el texto de búsqueda para {@code LIKE}: sin tildes, en minúsculas, con los
     * comodines escapados y rodeado de {@code %}. La columna se normaliza en la consulta
     * con {@code translate(lower(...))}, de modo que "perez" encuentra "Pérez".
     *
     * @param text texto escrito por el usuario (no vacío)
     * @return patrón para {@code like ... escape '!'}
     */
    static String likePattern(String text) {
        String plain = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String escaped = plain.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
