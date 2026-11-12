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
     * Cifras agregadas de un periodo en el comparativo; nunca nombra estudiantes.
     *
     * @param period            periodo
     * @param momentId          identificador del momento
     * @param momentName        nombre del momento
     * @param subject           asignatura
     * @param maxScore          puntaje máximo
     * @param gradedGroups      grupos con nota
     * @param gradedStudents    estudiantes con nota
     * @param averageGrade      nota promedio, o {@code null}
     * @param passingPercentage porcentaje de aprobados, o {@code null}
     * @param distribution      distribución de notas
     */
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

    /**
     * Periodo académico del reporte.
     *
     * @param id   identificador
     * @param code código
     */
    public record PeriodSummary(Long id, String code) {
    }

    /**
     * Franja de la distribución de notas, tomada de los parámetros globales y escalada a la nota máxima del momento.
     *
     * @param range      texto del rango, p. ej. "3,00 - 3,79"
     * @param label      nombre de la franja
     * @param min        nota mínima de la franja
     * @param max        nota máxima de la franja
     * @param count      estudiantes en la franja
     * @param percentage porcentaje sobre el máximo (0 a 100), o {@code null}
     */
    public record DistributionBucket(
            String range,
            String label,
            BigDecimal min,
            BigDecimal max,
            int count,
            BigDecimal percentage) {
    }

    /**
     * Un RAP en los dos periodos comparados.
     *
     * @param code            código
     * @param description     descripción del RAP
     * @param averageScoreA   promedio en el periodo A
     * @param maxScoreA       máximo en el periodo A
     * @param percentageA     logro en el periodo A
     * @param averageScoreB   promedio en el periodo B
     * @param maxScoreB       máximo en el periodo B
     * @param percentageB     logro en el periodo B
     * @param deltaPercentage diferencia de logro (B menos A)
     */
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
