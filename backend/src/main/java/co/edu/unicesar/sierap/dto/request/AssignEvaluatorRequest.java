package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Evaluador externo que el docente asigna a uno de sus grupos.
 *
 * @param evaluatorId evaluador externo activo
 */
public record AssignEvaluatorRequest(@NotNull(message = "El evaluador es obligatorio") Long evaluatorId) {
}
