package co.edu.unicesar.sierap.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reporte individual de un estudiante en un momento (Etapa 7).
 *
 * @param generatedAt          fecha y hora de generación
 * @param period               periodo académico
 * @param program              programa académico
 * @param moment               momento evaluativo
 * @param student              estudiante evaluado
 * @param group                grupo al que pertenece
 * @param finalResults         {@code true} cuando ya cerró la ventana de calificación (JSON: {@code final})
 * @param individualGrade      nota individual definitiva; {@code null} si aún no hay nota calculable
 * @param groupGrade           nota base del grupo
 * @param percentage           porcentaje de logro general sobre el puntaje máximo (0–100 %)
 * @param learningOutcomes     desempeño detallado en cada RAP
 * @param submittedEvaluations evaluaciones enviadas
 * @param pendingEvaluations   evaluaciones pendientes por enviar
 */
public record IndividualReportData(
        LocalDateTime generatedAt,
        PeriodSummary period,
        ProgramSummary program,
        MomentSummary moment,
        StudentSummary student,
        GroupSummary group,
        @JsonProperty("final") boolean finalResults,
        BigDecimal individualGrade,
        BigDecimal groupGrade,
        BigDecimal percentage,
        List<OutcomeScore> learningOutcomes,
        int submittedEvaluations,
        int pendingEvaluations) implements ReportData {

    /**
     * Periodo académico del reporte.
     *
     * @param id   identificador
     * @param code código
     */
    public record PeriodSummary(Long id, String code) {
    }

    /**
     * Programa académico del reporte.
     *
     * @param id   identificador
     * @param code código
     * @param name nombre
     */
    public record ProgramSummary(Long id, String code, String name) {
    }

    /**
     * Momento evaluativo del reporte y su puntaje máximo.
     *
     * @param id           identificador
     * @param momentNumber número del momento
     * @param name         nombre
     * @param subject      asignatura
     * @param maxScore     puntaje máximo
     */
    public record MomentSummary(Long id, int momentNumber, String name, String subject, BigDecimal maxScore) {
    }

    /**
     * Estudiante del reporte individual.
     *
     * @param id              identificador
     * @param studentCode     código estudiantil
     * @param fullName        nombre completo
     * @param currentSemester semestre que cursa
     */
    public record StudentSummary(Long id, String studentCode, String fullName, int currentSemester) {
    }

    /**
     * Grupo (proyecto de aula) del reporte.
     *
     * @param id           identificador
     * @param name         nombre
     * @param projectTitle título del proyecto, o {@code null}
     * @param teacherName  docente que habilitó el grupo
     */
    public record GroupSummary(Long id, String name, String projectTitle, String teacherName) {
    }

    /**
     * Desempeño en un RAP del momento.
     *
     * @param id          identificador
     * @param code        código
     * @param description descripción del RAP
     * @param score       puntaje con 2 decimales, o {@code null} si no hay nota
     * @param groupScore  puntaje del grupo, o {@code null}
     * @param maxScore    puntaje máximo
     * @param percentage  porcentaje sobre el máximo (0 a 100), o {@code null}
     */
    public record OutcomeScore(
            Long id,
            String code,
            String description,
            BigDecimal score,
            BigDecimal groupScore,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }
}
