package co.edu.unicesar.sierap.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reporte por grupo de proyecto de aula (Etapa 7).
 *
 * @param generatedAt          fecha y hora de generación
 * @param period               periodo académico
 * @param program              programa académico
 * @param moment               momento evaluativo
 * @param group                datos del grupo
 * @param finalResults         {@code true} cuando ya cerró la ventana de calificación (JSON: {@code final})
 * @param groupGrade           nota base del grupo
 * @param percentage           porcentaje de logro general del grupo (0–100 %)
 * @param learningOutcomes     desempeño grupal por cada RAP
 * @param members              integrantes con su nota individual definitiva y desglose por RAP
 * @param submittedEvaluations evaluaciones enviadas
 * @param pendingEvaluations   evaluaciones pendientes por enviar
 */
public record GroupReportData(
        LocalDateTime generatedAt,
        PeriodSummary period,
        ProgramSummary program,
        MomentSummary moment,
        GroupSummary group,
        @JsonProperty("final") boolean finalResults,
        BigDecimal groupGrade,
        BigDecimal percentage,
        List<OutcomeScore> learningOutcomes,
        List<MemberSummary> members,
        int submittedEvaluations,
        int pendingEvaluations) implements ReportData {

    public record PeriodSummary(Long id, String code) {
    }

    public record ProgramSummary(Long id, String code, String name) {
    }

    public record MomentSummary(Long id, int momentNumber, String name, String subject, BigDecimal maxScore) {
    }

    public record GroupSummary(Long id, String name, String projectTitle, String teacherName) {
    }

    public record OutcomeScore(
            Long id,
            String code,
            String description,
            BigDecimal score,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }

    public record MemberSummary(
            Long studentId,
            String studentCode,
            String fullName,
            BigDecimal individualGrade,
            boolean adjusted,
            List<MemberOutcomeScore> learningOutcomes) {
    }

    public record MemberOutcomeScore(
            Long id,
            String code,
            BigDecimal score,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }
}
