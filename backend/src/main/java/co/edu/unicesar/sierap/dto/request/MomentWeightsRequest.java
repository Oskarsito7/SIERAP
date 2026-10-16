package co.edu.unicesar.sierap.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para configurar los pesos de calificación de un momento (ADR-018).
 *
 * <p>Reemplaza todos los pesos existentes en el momento.</p>
 *
 * @param teacherWeight    porcentaje asignado al docente
 * @param evaluatorWeights lista de pesos por tipo de evaluador externo
 */
public record MomentWeightsRequest(
        @NotNull(message = "El peso del docente es obligatorio")
        BigDecimal teacherWeight,

        @Valid
        List<EvaluatorWeightItemRequest> evaluatorWeights) {

    /**
     * Peso asignado a un tipo de evaluador externo.
     *
     * @param evaluatorTypeId identificador del tipo de evaluador
     * @param weight          porcentaje asignado
     */
    public record EvaluatorWeightItemRequest(
            @NotNull(message = "El tipo de evaluador es obligatorio")
            Long evaluatorTypeId,

            @NotNull(message = "El peso es obligatorio")
            BigDecimal weight) {
    }
}
