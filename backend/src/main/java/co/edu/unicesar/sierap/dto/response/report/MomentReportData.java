package co.edu.unicesar.sierap.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reporte consolidado de un momento y programa académico (Etapa 7).
 *
 * @param generatedAt        fecha y hora de generación
 * @param period             periodo académico
 * @param program            programa académico
 * @param moment             momento evaluativo
 * @param finalResults       {@code true} cuando ya cerró la ventana de calificación (JSON: {@code final})
 * @param totalGroups        total de grupos habilitados en el momento
 * @param gradedGroups       grupos con al menos una evaluación enviada que cuente
 * @param totalStudents      total de estudiantes pertenecientes a grupos del momento
 * @param gradedStudents     estudiantes que tienen nota calculada
 * @param averageGroupGrade  promedio de las notas de grupo; {@code null} si ningún grupo tiene nota
 * @param averageGrade       promedio de las notas individuales definitivas; {@code null} si ningún estudiante tiene nota
 * @param minGrade           nota individual más baja
 * @param maxGrade           nota individual más alta
 * @param passingCount       cantidad de estudiantes aprobados (nota >= 3.00)
 * @param passingPercentage  porcentaje de aprobación sobre los estudiantes calificados (0–100 %)
 * @param learningOutcomes   promedio de desempeño en cada RAP del momento
 * @param distribution       histograma en los 4 rangos de la escala UPC (Bajo, Básico, Alto, Sobresaliente)
 */
public record MomentReportData(
        LocalDateTime generatedAt,
        PeriodSummary period,
        ProgramSummary program,
        MomentSummary moment,
        @JsonProperty("final") boolean finalResults,
        int totalGroups,
        int gradedGroups,
        int totalStudents,
        int gradedStudents,
        BigDecimal averageGroupGrade,
        BigDecimal averageGrade,
        BigDecimal minGrade,
        BigDecimal maxGrade,
        int passingCount,
        BigDecimal passingPercentage,
        List<OutcomeSummary> learningOutcomes,
        List<DistributionBucket> distribution) implements ReportData {

    public record PeriodSummary(Long id, String code) {
    }

    public record ProgramSummary(Long id, String code, String name) {
    }

    public record MomentSummary(Long id, int momentNumber, String name, String subject, BigDecimal maxScore) {
    }

    public record OutcomeSummary(
            Long id,
            String code,
            String description,
            BigDecimal averageScore,
            BigDecimal maxScore,
            BigDecimal percentage) {
    }

    public record DistributionBucket(
            String range,
            String label,
            BigDecimal min,
            BigDecimal max,
            int count,
            BigDecimal percentage) {
    }
}
