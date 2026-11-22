package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Una página de resultados junto con el total de elementos que cumplen el filtro
 * (ADR-024). El controller entrega {@code items} como cuerpo y {@code total} en la
 * cabecera {@code X-Total-Count}.
 *
 * @param items elementos de la página pedida, en el orden de siempre de la lista
 * @param total cantidad total de elementos, sin paginar
 * @param <T>   tipo de los elementos
 */
public record PagedResult<T>(List<T> items, long total) {
}
