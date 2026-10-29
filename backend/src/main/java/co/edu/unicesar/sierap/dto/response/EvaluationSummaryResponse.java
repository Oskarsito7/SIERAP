package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;

/**
 * Evaluación de un grupo en la lista de evaluaciones
 * ({@code GET /api/groups/{id}/evaluations}, {@code docs/07}).
 *
 * @param id          identificador
 * @param evaluator   quién califica y de qué tipo es
 * @param status      borrador o enviada
 * @param evaluatedAt fecha y hora de la última modificación
 * @param totalScore  nota del grupo en esta evaluación (solo niveles del grupo)
 * @param comment     observación general
 */
public record EvaluationSummaryResponse(
        Long id,
        EvaluatorRef evaluator,
        EvaluationStatus status,
        LocalDateTime evaluatedAt,
        BigDecimal totalScore,
        String comment) {

    /**
     * Evaluador de la evaluación.
     *
     * @param id       identificador del usuario
     * @param fullName nombre completo
     * @param kind     "Docente" o el nombre del tipo de evaluador, p. ej. "Empresario"
     */
    public record EvaluatorRef(Long id, String fullName, String kind) {
    }
}
