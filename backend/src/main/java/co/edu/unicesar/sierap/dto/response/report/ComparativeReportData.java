package co.edu.unicesar.sierap.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte comparativo entre dos semestres o periodos académicos para el mismo programa y número de momento (Etapa 7).
 * Estrictamente agregado (sin nombres de estudiantes).
 *
 * @param generatedAt            fecha y hora de generación
 * @param program                programa académico comparado
 * @param momentNumber           número de momento (p. ej. 1)
 * @param periodA                métricas agregadas del periodo base A
 * @param periodB                métricas agregadas del periodo objetivo B
 * @param deltaAverageGrade      cambio en el promedio de notas (periodB - periodA)
 * @param deltaPassingPercentage cambio en la tasa de aprobación en puntos porcentuales (periodB - periodA)
 * @param learningOutcomes       comparativa por RAP emparejada por código
 */
public record ComparativeReportData(
        LocalDateTime generatedAt,
        ProgramSummary program,
        int momentNumber,
        PeriodComparisonSummary periodA,
        PeriodComparisonSummary periodB,
        BigDecimal deltaAverageGrade,
        BigDecimal deltaPassingPercentage,
        List<OutcomeComparison> learningOutcomes) implements ReportData {

    public record ProgramSummary(Long id, String code, String name) {
    }

    public record PeriodComparisonSummary(
            PeriodSummary period,
            Long momentId,
            String momentName,
            String subject,
            BigDecimal maxScore,
            int gradedGroups,
            int gradedStudents,
            BigDecimal averageGrade,
            BigDecimal passingPercentage,
            List<DistributionBucket> distribution) {
    }

    public record PeriodSummary(Long id, String code) {
    }

    public record DistributionBucket(
            String range,
            String label,
            BigDecimal min,
            BigDecimal max,
            int count,
            BigDecimal percentage) {
    }

    public record OutcomeComparison(
            String code,
            String description,
            BigDecimal averageScoreA,
            BigDecimal maxScoreA,
            BigDecimal percentageA,
            BigDecimal averageScoreB,
            BigDecimal maxScoreB,
            BigDecimal percentageB,
            BigDecimal deltaPercentage) {
    }
}
