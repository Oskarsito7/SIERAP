package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta con la distribución de pesos de un momento (ADR-018).
 *
 * @param teacherWeight    porcentaje asignado al docente
 * @param evaluatorWeights pesos asignados a cada tipo de evaluador externo
 * @param totalWeight      suma total de los pesos (docente más evaluadores)
 */
public record MomentWeightsResponse(
        BigDecimal teacherWeight,
        List<EvaluatorWeightItemResponse> evaluatorWeights,
        BigDecimal totalWeight) {

    /**
     * Peso de un tipo de evaluador externo.
     *
     * @param evaluatorTypeId   identificador del tipo de evaluador
     * @param evaluatorTypeName nombre visible del tipo
     * @param weight            porcentaje asignado
     */
    public record EvaluatorWeightItemResponse(
            Long evaluatorTypeId,
            String evaluatorTypeName,
            BigDecimal weight) {
    }
}
