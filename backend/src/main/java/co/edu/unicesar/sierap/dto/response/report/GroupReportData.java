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
     * @param maxScore    puntaje máximo
     * @param percentage  porcentaje sobre el máximo (0 a 100), o {@code null}
     */
    public record OutcomeScore(
            Long id,
            String code,
            String description,
            BigDecimal score,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }

    /**
     * Integrante del grupo con su nota individual.
     *
     * @param studentId        identificador del estudiante
     * @param studentCode      código estudiantil
     * @param fullName         nombre completo
     * @param individualGrade  nota individual, o {@code null}
     * @param adjusted         {@code true} si un evaluador ajustó su nota
     * @param learningOutcomes desempeño por RAP
     */
    public record MemberSummary(
            Long studentId,
            String studentCode,
            String fullName,
            BigDecimal individualGrade,
            boolean adjusted,
            List<MemberOutcomeScore> learningOutcomes) {
    }

    /**
     * Desempeño de un integrante en un RAP.
     *
     * @param id         identificador
     * @param code       código
     * @param score      puntaje con 2 decimales, o {@code null} si no hay nota
     * @param maxScore   puntaje máximo
     * @param percentage porcentaje sobre el máximo (0 a 100), o {@code null}
     */
    public record MemberOutcomeScore(
            Long id,
            String code,
            BigDecimal score,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }
}
