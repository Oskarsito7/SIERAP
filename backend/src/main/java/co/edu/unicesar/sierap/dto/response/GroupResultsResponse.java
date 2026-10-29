package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultados de un grupo ({@code GroupResults} en {@code docs/07}): la nota del grupo y
 * la individual de cada integrante, en total y por RAP. Mientras la ventana de
 * calificación está abierta es una vista previa con las evaluaciones enviadas hasta hoy.
 *
 * @param groupId               grupo
 * @param finalResults          {@code true} cuando ya cerró la ventana de calificación
 *                              (en el JSON, {@code final})
 * @param maxScore              puntaje máximo del momento
 * @param groupGrade            nota del grupo; {@code null} si no hay evaluaciones enviadas que cuenten
 * @param groupLearningOutcomes puntaje del grupo en cada RAP
 * @param members               nota de cada integrante, ordenados por apellidos
 * @param submittedEvaluations  evaluaciones enviadas
 * @param pendingEvaluations    evaluadores (el docente y los externos asignados) que aún no envían
 */
public record GroupResultsResponse(
        Long groupId,
        @JsonProperty("final") boolean finalResults,
        BigDecimal maxScore,
        BigDecimal groupGrade,
        List<OutcomeScoreResponse> groupLearningOutcomes,
        List<MemberResultResponse> members,
        int submittedEvaluations,
        int pendingEvaluations) {

    /**
     * Puntaje en un RAP sobre su máximo.
     *
     * @param id       RAP
     * @param code     código del RAP, p. ej. {@code RAP1}
     * @param score    puntaje obtenido; {@code null} si aún no hay nota
     * @param maxScore puntaje máximo del RAP
     */
    public record OutcomeScoreResponse(Long id, String code, BigDecimal score, BigDecimal maxScore) {
    }

    /**
     * Nota individual de un integrante: la del grupo con sus ajustes (ADR-018).
     *
     * @param studentId        estudiante
     * @param fullName         nombre completo
     * @param individualGrade  su nota; {@code null} si aún no hay nota
     * @param learningOutcomes su puntaje en cada RAP
     */
    public record MemberResultResponse(Long studentId, String fullName, BigDecimal individualGrade,
                                       List<OutcomeScoreResponse> learningOutcomes) {
    }
}
