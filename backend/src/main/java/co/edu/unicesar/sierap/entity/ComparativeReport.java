package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.ComparativeReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;

/**
 * Reporte comparativo entre dos periodos académicos (semestre X frente a semestre Y)
 * para el mismo programa y número de momento (Etapa 7).
 *
 * <p>Maneja datos estrictamente agregados sin nombres de estudiantes.</p>
 */
public class ComparativeReport extends Report {

    private final Moment momentA;
    private final List<ProjectGroup> groupsA;
    private final Moment momentB;
    private final List<ProjectGroup> groupsB;
    private final LocalDate today;
    private final GradeScale gradeScale;

    /**
     * Construye un reporte comparativo con la escala de calificación configurada.
     *
     * @param generatedAt marca de tiempo de generación
     * @param momentA     momento del periodo base A
     * @param groupsA     grupos del periodo A
     * @param momentB     momento del periodo objetivo B
     * @param groupsB     grupos del periodo B
     * @param today       fecha actual
     * @param gradeScale  escala de calificación para aprobación y distribución
     */
    public ComparativeReport(
            LocalDateTime generatedAt,
            Moment momentA,
            List<ProjectGroup> groupsA,
            Moment momentB,
            List<ProjectGroup> groupsB,
            LocalDate today,
            GradeScale gradeScale) {
        super(generatedAt, momentB.getPeriod());
        this.momentA = Objects.requireNonNull(momentA, "momentA no puede ser nulo");
        this.groupsA = Objects.requireNonNull(groupsA, "groupsA no puede ser nulo");
        this.momentB = Objects.requireNonNull(momentB, "momentB no puede ser nulo");
        this.groupsB = Objects.requireNonNull(groupsB, "groupsB no puede ser nulo");
        this.today = Objects.requireNonNull(today, "today no puede ser nulo");
        this.gradeScale = Objects.requireNonNull(gradeScale, "gradeScale no puede ser nulo");
    }

    /**
     * Momento del periodo base A.
     *
     * @return momento A
     */
    public Moment getMomentA() {
        return momentA;
    }

    /**
     * Momento del periodo objetivo B.
     *
     * @return momento B
     */
    public Moment getMomentB() {
        return momentB;
    }

    /**
     * Escala de calificación aplicada en la comparación.
     *
     * @return escala de calificación
     */
    public GradeScale getGradeScale() {
        return gradeScale;
    }

    @Override
    public ComparativeReportData generate() {
        MomentReportData dataA = new MomentReport(getGeneratedAt(), momentA, groupsA, today, gradeScale).generate();
        MomentReportData dataB = new MomentReport(getGeneratedAt(), momentB, groupsB, today, gradeScale).generate();

        ComparativeReportData.PeriodComparisonSummary summaryA = toPeriodSummary(dataA);
        ComparativeReportData.PeriodComparisonSummary summaryB = toPeriodSummary(dataB);

        BigDecimal deltaAverageGrade = null;
        if (dataB.averageGrade() != null && dataA.averageGrade() != null) {
            deltaAverageGrade = dataB.averageGrade().subtract(dataA.averageGrade());
        }

        BigDecimal deltaPassingPercentage = null;
        if (dataB.passingPercentage() != null && dataA.passingPercentage() != null) {
            deltaPassingPercentage = dataB.passingPercentage().subtract(dataA.passingPercentage());
        }

        List<ComparativeReportData.OutcomeComparison> outcomes = pairOutcomes(dataA, dataB);

        Program program = momentB.getProgram();
        return new ComparativeReportData(
                getGeneratedAt(),
                new ComparativeReportData.ProgramSummary(program.getId(), program.getCode(), program.getName()),
                momentB.getNumber(),
                summaryA,
                summaryB,
                deltaAverageGrade,
                deltaPassingPercentage,
                outcomes
        );
    }

    private static ComparativeReportData.PeriodComparisonSummary toPeriodSummary(MomentReportData data) {
        List<ComparativeReportData.DistributionBucket> dist = data.distribution().stream()
                .map(b -> new ComparativeReportData.DistributionBucket(
                        b.range(), b.label(), b.min(), b.max(), b.count(), b.percentage()))
                .toList();

        return new ComparativeReportData.PeriodComparisonSummary(
                new ComparativeReportData.PeriodSummary(data.period().id(), data.period().code()),
                data.moment().id(),
                data.moment().name(),
                data.moment().subject(),
                data.moment().maxScore(),
                data.gradedGroups(),
                data.gradedStudents(),
                data.averageGrade(),
                data.passingPercentage(),
                dist
        );
    }

    private static List<ComparativeReportData.OutcomeComparison> pairOutcomes(
            MomentReportData dataA,
            MomentReportData dataB) {

        Map<String, MomentReportData.OutcomeSummary> mapA = new LinkedHashMap<>();
        for (MomentReportData.OutcomeSummary o : dataA.learningOutcomes()) {
            mapA.put(o.code().toUpperCase(), o);
        }

        Map<String, MomentReportData.OutcomeSummary> mapB = new LinkedHashMap<>();
        for (MomentReportData.OutcomeSummary o : dataB.learningOutcomes()) {
            mapB.put(o.code().toUpperCase(), o);
        }

        List<ComparativeReportData.OutcomeComparison> result = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (MomentReportData.OutcomeSummary b : dataB.learningOutcomes()) {
            String key = b.code().toUpperCase();
            seen.add(key);
            MomentReportData.OutcomeSummary a = mapA.get(key);

            BigDecimal avgA = a != null ? a.averageScore() : null;
            BigDecimal maxA = a != null ? a.maxScore() : null;
            BigDecimal pctA = a != null ? a.percentage() : null;

            BigDecimal avgB = b.averageScore();
            BigDecimal maxB = b.maxScore();
            BigDecimal pctB = b.percentage();

            BigDecimal deltaPct = null;
            if (pctB != null && pctA != null) {
                deltaPct = pctB.subtract(pctA);
            }

            result.add(new ComparativeReportData.OutcomeComparison(
                    b.code(),
                    b.description(),
                    avgA,
                    maxA,
                    pctA,
                    avgB,
                    maxB,
                    pctB,
                    deltaPct
            ));
        }

        for (MomentReportData.OutcomeSummary a : dataA.learningOutcomes()) {
            String key = a.code().toUpperCase();
            if (!seen.contains(key)) {
                result.add(new ComparativeReportData.OutcomeComparison(
                        a.code(),
                        a.description(),
                        a.averageScore(),
                        a.maxScore(),
                        a.percentage(),
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        return result;
    }
}
