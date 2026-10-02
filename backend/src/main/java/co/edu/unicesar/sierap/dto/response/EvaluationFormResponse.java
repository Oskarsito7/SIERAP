package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;

/**
 * Evaluación propia de un grupo, o una vacía para empezar ({@code EvaluationForm} en
 * {@code docs/07}). Trae la estructura del momento con el nivel elegido en cada
 * criterio y la nota de cada integrante con sus ajustes (ADR-018).
 *
 * @param id                {@code null} si todavía no se empezó
 * @param groupId           grupo evaluado
 * @param status            {@code null} si todavía no se empezó
 * @param evaluatedAt       fecha y hora de la última modificación; {@code null} si no se empezó
 * @param comment           observación general para el grupo
 * @param canEdit           {@code true} si hoy está abierto el periodo de calificación del grupo
 * @param submissionToGrade versión de la entrega que se califica; {@code null} si el grupo no entregó
 * @param learningOutcomes  RAP con sus criterios y el nivel del grupo en cada uno
 * @param members           integrantes con sus ajustes y su nota
 * @param totalScore        nota del grupo, solo con los niveles del grupo
 * @param maxScore          puntaje máximo del momento
 * @param complete          {@code true} si todos los criterios tienen nivel del grupo
 */
public record EvaluationFormResponse(
        Long id,
        Long groupId,
        EvaluationStatus status,
        LocalDateTime evaluatedAt,
        String comment,
        boolean canEdit,
        SubmissionRef submissionToGrade,
        List<OutcomeForm> learningOutcomes,
        List<MemberForm> members,
        BigDecimal totalScore,
        BigDecimal maxScore,
        boolean complete) {

    /**
     * Versión de la entrega que se califica.
     *
     * @param id            identificador de la versión
     * @param versionNumber número de versión
     * @param late          {@code true} si llegó después de la fecha límite del grupo
     */
    public record SubmissionRef(Long id, int versionNumber, boolean late) {
    }

    /**
     * RAP con sus criterios.
     *
     * @param id          identificador
     * @param code        código, p. ej. {@code RAP1}
     * @param description enunciado
     * @param maxScore    puntaje máximo del RAP
     * @param criteria    criterios con el nivel del grupo
     */
    public record OutcomeForm(Long id, String code, String description, BigDecimal maxScore,
                              List<CriterionForm> criteria) {
    }

    /**
     * Criterio con sus niveles y el nivel elegido para el grupo.
     *
     * @param id          identificador
     * @param description descripción del criterio
     * @param levels      niveles de desempeño
     * @param levelId     nivel del grupo; {@code null} si todavía no se calificó
     */
    public record CriterionForm(Long id, String description, List<LevelResponse> levels, Long levelId) {
    }

    /**
     * Integrante del grupo con su nota individual.
     *
     * @param id          identificador del estudiante
     * @param fullName    nombre completo
     * @param adjustments solo los criterios en que su nivel difiere del grupo
     * @param totalScore  su nota: la del grupo con sus ajustes
     */
    public record MemberForm(Long id, String fullName, List<Adjustment> adjustments, BigDecimal totalScore) {
    }

    /**
     * Ajuste individual de un criterio.
     *
     * @param criterionId criterio ajustado
     * @param levelId     nivel para ese estudiante
     */
    public record Adjustment(Long criterionId, Long levelId) {
    }
}
