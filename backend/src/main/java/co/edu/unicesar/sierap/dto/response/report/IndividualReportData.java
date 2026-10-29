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

    public record PeriodSummary(Long id, String code) {
    }

    public record ProgramSummary(Long id, String code, String name) {
    }

    public record MomentSummary(Long id, int momentNumber, String name, String subject, BigDecimal maxScore) {
    }

    public record StudentSummary(Long id, String studentCode, String fullName, int currentSemester) {
    }

    public record GroupSummary(Long id, String name, String projectTitle, String teacherName) {
    }

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
