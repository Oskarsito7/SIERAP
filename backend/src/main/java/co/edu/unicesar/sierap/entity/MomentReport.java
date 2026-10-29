package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.MomentReportData;

/**
 * Reporte consolidado de un momento y programa académico (Etapa 7).
 *
 * <p>Consolida el rendimiento de todos los grupos y estudiantes evaluados del momento,
 * calculando estadísticas agregadas, promedios por RAP e histograma de distribución en 4 rangos.</p>
 */
public class MomentReport extends Report {

    private static final MathContext PRECISION = MathContext.DECIMAL64;
    private static final BigDecimal PASSING_THRESHOLD = new BigDecimal("3.00");

    private final Moment moment;
    private final List<ProjectGroup> groups;
    private final LocalDate today;

    /**
     * Construye un reporte consolidado de momento.
     *
     * @param generatedAt marca de tiempo de generación
     * @param moment      momento consultado
     * @param groups      grupos del momento
     * @param today       fecha actual
     */
    public MomentReport(LocalDateTime generatedAt, Moment moment, List<ProjectGroup> groups, LocalDate today) {
        super(generatedAt, moment.getPeriod());
        this.moment = Objects.requireNonNull(moment, "moment no puede ser nulo");
        this.groups = Objects.requireNonNull(groups, "groups no puede ser nulo");
        this.today = Objects.requireNonNull(today, "today no puede ser nulo");
    }

    /**
     * Momento evaluativo del reporte.
     *
     * @return momento
     */
    public Moment getMoment() {
        return moment;
    }

    /**
     * Grupos incluidos en la consolidación.
     *
     * @return lista de grupos
     */
    public List<ProjectGroup> getGroups() {
        return groups;
    }

    @Override
    public MomentReportData generate() {
        LocalDate gradingEnd = moment.getGradingEnd();
        boolean finalResults = gradingEnd != null && today.isAfter(gradingEnd);

        int totalGroups = groups.size();
        List<BigDecimal> groupGrades = new ArrayList<>();
        List<BigDecimal> studentGrades = new ArrayList<>();
        int totalStudents = 0;

        for (ProjectGroup group : groups) {
            totalStudents += group.getMembers().size();
            group.getGroupGrade().ifPresent(groupGrades::add);

            for (Student student : group.getMembers()) {
                group.getIndividualGrade(student).ifPresent(studentGrades::add);
            }
        }

        int gradedGroups = groupGrades.size();
        int gradedStudents = studentGrades.size();

        BigDecimal averageGroupGrade = null;
        if (!groupGrades.isEmpty()) {
            BigDecimal sum = groupGrades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            averageGroupGrade = sum.divide(BigDecimal.valueOf(groupGrades.size()), PRECISION)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageGrade = null;
        BigDecimal minGrade = null;
        BigDecimal maxGrade = null;
        int passingCount = 0;
        BigDecimal passingPercentage = null;

        if (!studentGrades.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal g : studentGrades) {
                sum = sum.add(g);
                if (minGrade == null || g.compareTo(minGrade) < 0) {
                    minGrade = g;
                }
                if (maxGrade == null || g.compareTo(maxGrade) > 0) {
                    maxGrade = g;
                }
                if (g.compareTo(PASSING_THRESHOLD) >= 0) {
                    passingCount++;
                }
            }
            averageGrade = sum.divide(BigDecimal.valueOf(studentGrades.size()), PRECISION)
                    .setScale(2, RoundingMode.HALF_UP);
            minGrade = minGrade.setScale(2, RoundingMode.HALF_UP);
            maxGrade = maxGrade.setScale(2, RoundingMode.HALF_UP);
            passingPercentage = BigDecimal.valueOf(passingCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(gradedStudents), 2, RoundingMode.HALF_UP);
        }

        List<MomentReportData.OutcomeSummary> outcomes = new ArrayList<>();
        for (LearningOutcome outcome : moment.getLearningOutcomes()) {
            List<BigDecimal> indScores = new ArrayList<>();
            for (ProjectGroup group : groups) {
                for (Student student : group.getMembers()) {
                    group.getIndividualScoreFor(outcome, student).ifPresent(indScores::add);
                }
            }

            BigDecimal avgScore = null;
            BigDecimal percentage = null;
            BigDecimal maxScore = outcome.getMaxScore();

            if (!indScores.isEmpty()) {
                BigDecimal sum = indScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                avgScore = sum.divide(BigDecimal.valueOf(indScores.size()), PRECISION)
                        .setScale(2, RoundingMode.HALF_UP);
                if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                    percentage = avgScore.multiply(BigDecimal.valueOf(100))
                            .divide(maxScore, 2, RoundingMode.HALF_UP);
                }
            }

            outcomes.add(new MomentReportData.OutcomeSummary(
                    outcome.getId(),
                    outcome.getCode(),
                    outcome.getDescription(),
                    avgScore,
                    maxScore,
                    percentage
            ));
        }

        List<MomentReportData.DistributionBucket> distribution = calculateDistribution(studentGrades, gradedStudents);

        return new MomentReportData(
                getGeneratedAt(),
                new MomentReportData.PeriodSummary(getPeriod().getId(), getPeriod().getCode()),
                new MomentReportData.ProgramSummary(
                        moment.getProgram().getId(),
                        moment.getProgram().getCode(),
                        moment.getProgram().getName()),
                new MomentReportData.MomentSummary(
                        moment.getId(),
                        moment.getNumber(),
                        moment.getName(),
                        moment.getSubject(),
                        moment.getMaxScore()),
                finalResults,
                totalGroups,
                gradedGroups,
                totalStudents,
                gradedStudents,
                averageGroupGrade,
                averageGrade,
                minGrade,
                maxGrade,
                passingCount,
                passingPercentage,
                outcomes,
                distribution
        );
    }

    private static List<MomentReportData.DistributionBucket> calculateDistribution(List<BigDecimal> grades, int totalGraded) {
        BucketConfig[] configs = new BucketConfig[] {
                new BucketConfig("0.00 - 2.99", "Bajo", new BigDecimal("0.00"), new BigDecimal("2.99")),
                new BucketConfig("3.00 - 3.79", "Básico", new BigDecimal("3.00"), new BigDecimal("3.79")),
                new BucketConfig("3.80 - 4.49", "Alto", new BigDecimal("3.80"), new BigDecimal("4.49")),
                new BucketConfig("4.50 - 5.00", "Sobresaliente", new BigDecimal("4.50"), new BigDecimal("5.00"))
        };

        List<MomentReportData.DistributionBucket> result = new ArrayList<>();
        for (BucketConfig cfg : configs) {
            int count = 0;
            for (BigDecimal g : grades) {
                if (g.compareTo(cfg.min) >= 0 && g.compareTo(cfg.max) <= 0) {
                    count++;
                }
            }
            BigDecimal pct = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalGraded > 0) {
                pct = BigDecimal.valueOf(count)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalGraded), 2, RoundingMode.HALF_UP);
            }
            result.add(new MomentReportData.DistributionBucket(cfg.range, cfg.label, cfg.min, cfg.max, count, pct));
        }
        return result;
    }

    private record BucketConfig(String range, String label, BigDecimal min, BigDecimal max) {
    }
}
