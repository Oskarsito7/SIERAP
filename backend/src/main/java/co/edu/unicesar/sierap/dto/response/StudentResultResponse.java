package co.edu.unicesar.sierap.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que ve un estudiante de un momento ({@code StudentResult} en {@code docs/07}). Las
 * notas, los RAP y los comentarios solo llegan cuando cierra la ventana de calificación;
 * antes las notas son {@code null} y las listas están vacías.
 *
 * @param momentId         momento
 * @param momentName       nombre del momento
 * @param periodCode       código del periodo académico, p. ej. {@code 2026-2}
 * @param groupId          grupo del estudiante en ese momento
 * @param groupName        nombre del grupo
 * @param projectTitle     título del proyecto de aula; puede ser {@code null}
 * @param available        {@code true} cuando ya cerró la ventana de calificación
 * @param finalGrade       su nota individual, la definitiva
 * @param groupGrade       la nota del grupo
 * @param maxScore         puntaje máximo del momento
 * @param learningOutcomes su desempeño en cada RAP
 * @param comments         comentarios de los evaluadores, con su tipo y no su nombre
 */
public record StudentResultResponse(
        Long momentId,
        String momentName,
        String periodCode,
        Long groupId,
        String groupName,
        String projectTitle,
        boolean available,
        BigDecimal finalGrade,
        BigDecimal groupGrade,
        BigDecimal maxScore,
        List<StudentOutcomeScoreResponse> learningOutcomes,
        List<EvaluatorCommentResponse> comments) {

    /**
     * Desempeño del estudiante en un RAP.
     *
     * @param code        código del RAP
     * @param description enunciado del RAP
     * @param score       su puntaje; {@code null} si no hay evaluaciones enviadas que cuenten
     * @param maxScore    puntaje máximo del RAP
     * @param criteria    su puntaje consolidado en cada criterio del RAP, por orden de aparición
     */
    public record StudentOutcomeScoreResponse(String code, String description, BigDecimal score,
                                              BigDecimal maxScore, List<StudentCriterionScoreResponse> criteria) {
    }

    /**
     * Puntaje consolidado del estudiante en un criterio: el que cuenta en su nota, con el
     * mismo cálculo del RAP (promedio por tipo de evaluador, ponderado, con sus ajustes).
     * Nunca se exponen los niveles que eligió cada evaluador ni quién eligió qué. Como
     * cada criterio se redondea a 2 decimales por separado, la suma de los criterios
     * puede diferir en 0,01 del puntaje del RAP.
     *
     * @param description enunciado del criterio
     * @param score       su puntaje; {@code null} si no hay evaluaciones enviadas que cuenten
     * @param maxScore    puntaje máximo del criterio
     */
    public record StudentCriterionScoreResponse(String description, BigDecimal score, BigDecimal maxScore) {
    }

    /**
     * Comentario de una evaluación enviada. Nunca incluye los niveles que eligió el evaluador.
     *
     * @param evaluatorKind "Docente" o el nombre del tipo del evaluador externo
     * @param comment       comentario
     */
    public record EvaluatorCommentResponse(String evaluatorKind, String comment) {
    }
}
