package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Petición para copiar la configuración de momentos de un periodo académico a otro (ADR-010).
 *
 * @param fromPeriodId identificador del periodo académico origen
 * @param toPeriodId   identificador del periodo académico destino
 * @param programId    identificador opcional de un programa específico; si se omite,
 *                     se copian todos los programas en el alcance del usuario
 */
public record CopyMomentsRequest(
        @NotNull(message = "El periodo origen es obligatorio.")
        Long fromPeriodId,

        @NotNull(message = "El periodo destino es obligatorio.")
        Long toPeriodId,

        Long programId) {
}
