package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.PagedResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * Convención de paginación opcional de las listas (ADR-024): los parámetros
 * {@code page} (desde 0) y {@code size} (1 a 200) son opcionales; si no llegan, la
 * respuesta es la lista completa de siempre. Si llegan, el cuerpo sigue siendo un arreglo
 * (la página pedida) y la cabecera {@code X-Total-Count} trae el total.
 */
final class PagedResponses {

    /** Cabecera con el total de elementos, sin paginar. */
    static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    /** Tamaño de página mínimo permitido. */
    static final int MIN_SIZE = 1;

    /** Tamaño de página máximo permitido. */
    static final int MAX_SIZE = 200;

    /** Tamaño de página si solo llega {@code page}. */
    static final int DEFAULT_SIZE = 50;

    private PagedResponses() {
    }

    /**
     * Indica si la petición pidió una página (llegó {@code page} o {@code size}).
     *
     * @param page número de página recibido, o {@code null}
     * @param size tamaño recibido, o {@code null}
     * @return {@code true} si hay paginación
     */
    static boolean isPaged(Integer page, Integer size) {
        return page != null || size != null;
    }

    /**
     * Arma la paginación pedida; el orden lo aplica cada service.
     *
     * @param page número de página (desde 0), o {@code null} para la primera
     * @param size tamaño de página, o {@code null} para el predeterminado
     * @return la página pedida, o {@link Pageable#unpaged()} si no llegó ninguno
     */
    static Pageable pageable(Integer page, Integer size) {
        if (!isPaged(page, size)) {
            return Pageable.unpaged();
        }
        return PageRequest.of(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    /**
     * Responde el arreglo de elementos y, solo si hubo paginación, el total en
     * {@code X-Total-Count}.
     *
     * @param result resultado del service
     * @param paged  si la petición pidió una página
     * @param <T>    tipo de los elementos
     * @return respuesta {@code 200}
     */
    static <T> ResponseEntity<List<T>> respond(PagedResult<T> result, boolean paged) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (paged) {
            builder.header(TOTAL_COUNT_HEADER, String.valueOf(result.total()));
        }
        return builder.body(result.items());
    }
}
