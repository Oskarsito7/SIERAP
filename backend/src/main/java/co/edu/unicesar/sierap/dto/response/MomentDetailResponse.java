package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Detalle de un momento ({@code MomentDetail} en {@code docs/07}): los datos del
 * resumen más la estructura de evaluación (pesos, RAP, criterios y niveles).
 *
 * <p>Los campos del resumen se escriben en el mismo nivel del JSON, no dentro de un
 * objeto {@code summary}, para que el detalle sea un {@code MomentSummary} con más
 * campos.</p>
 *
 * @param summary          datos del resumen del momento
 * @param teacherWeight    porcentaje de la nota final que aporta el docente
 * @param evaluatorWeights pesos de los tipos de evaluador externo en el momento
 * @param totalWeight      suma total de los pesos (docente más evaluadores)
 * @param maxScore         puntaje máximo del momento (suma de sus RAP)
 * @param learningOutcomes RAP del momento, ordenados por código
 */
public record MomentDetailResponse(
        @JsonUnwrapped MomentSummaryResponse summary,
        BigDecimal teacherWeight,
        List<EvaluatorWeightItemResponse> evaluatorWeights,
        BigDecimal totalWeight,
        BigDecimal maxScore,
        List<LearningOutcomeResponse> learningOutcomes) {

    /**
     * Peso de un tipo de evaluador externo en el momento.
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

    /**
     * RAP del momento con sus criterios.
     *
     * @param id          identificador
     * @param code        código del RAP, p. ej. {@code RAP1}
     * @param description enunciado del resultado de aprendizaje
     * @param maxScore    puntaje máximo del RAP (suma de sus criterios)
     * @param criteria    criterios, en su orden de presentación
     */
    public record LearningOutcomeResponse(
            Long id,
            String code,
            String description,
            BigDecimal maxScore,
            List<CriterionResponse> criteria) {
    }

    /**
     * Criterio de evaluación de un RAP con sus niveles de desempeño.
     *
     * @param id           identificador
     * @param description  descripción del criterio
     * @param displayOrder posición del criterio dentro del RAP
     * @param maxScore     puntaje máximo del criterio (el de su nivel más alto)
     * @param levels       niveles de desempeño, en su orden de presentación
     */
    public record CriterionResponse(
            Long id,
            String description,
            int displayOrder,
            BigDecimal maxScore,
            List<LevelResponse> levels) {
    }

    /**
     * Nivel de desempeño de un criterio.
     *
     * @param id           identificador
     * @param name         nombre del nivel, p. ej. "Bajo"
     * @param description  descripción del desempeño esperado, o {@code null}
     * @param score        puntaje que otorga el nivel
     * @param displayOrder posición del nivel dentro del criterio
     */
    public record LevelResponse(
            Long id,
            String name,
            String description,
            BigDecimal score,
            int displayOrder) {
    }
}
