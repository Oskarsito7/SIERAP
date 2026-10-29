package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;

/**
 * Reporte individual del desempeño de un estudiante por RAP en un momento (Etapa 7).
 */
public class IndividualReport extends Report {

    private final Student student;
    private final ProjectGroup group;
    private final LocalDate today;

    /**
     * Construye un reporte individual.
     *
     * @param generatedAt marca de tiempo de generación
     * @param student     estudiante consultado
     * @param group       grupo al que pertenece el estudiante en el momento
     * @param today       fecha actual para verificar si cerró la ventana de calificación
     */
    public IndividualReport(LocalDateTime generatedAt, Student student, ProjectGroup group, LocalDate today) {
        super(generatedAt, group.getMoment().getPeriod());
        this.student = Objects.requireNonNull(student, "student no puede ser nulo");
        this.group = Objects.requireNonNull(group, "group no puede ser nulo");
        this.today = Objects.requireNonNull(today, "today no puede ser nulo");
    }

    /**
     * Estudiante evaluado en este reporte.
     *
     * @return estudiante
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Grupo al que pertenece el estudiante.
     *
     * @return grupo
     */
    public ProjectGroup getGroup() {
        return group;
    }

    @Override
    public IndividualReportData generate() {
        Moment moment = group.getMoment();
        LocalDate gradingEnd = moment.getGradingEnd();
        boolean finalResults = gradingEnd != null && today.isAfter(gradingEnd);

        BigDecimal individualGrade = group.getIndividualGrade(student).orElse(null);
        BigDecimal groupGrade = group.getGroupGrade().orElse(null);
        BigDecimal maxScore = moment.getMaxScore();

        BigDecimal percentage = null;
        if (individualGrade != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            percentage = individualGrade.multiply(BigDecimal.valueOf(100))
                    .divide(maxScore, 2, RoundingMode.HALF_UP);
        }

        List<IndividualReportData.OutcomeScore> outcomes = new ArrayList<>();
        for (LearningOutcome outcome : moment.getLearningOutcomes()) {
            BigDecimal indScore = group.getIndividualScoreFor(outcome, student).orElse(null);
            BigDecimal grpScore = group.getGroupScoreFor(outcome).orElse(null);
            BigDecimal outcomeMax = outcome.getMaxScore();
            BigDecimal outcomePercentage = null;
            if (indScore != null && outcomeMax != null && outcomeMax.compareTo(BigDecimal.ZERO) > 0) {
                outcomePercentage = indScore.multiply(BigDecimal.valueOf(100))
                        .divide(outcomeMax, 2, RoundingMode.HALF_UP);
            }
            outcomes.add(new IndividualReportData.OutcomeScore(
                    outcome.getId(),
                    outcome.getCode(),
                    outcome.getDescription(),
                    indScore,
                    grpScore,
                    outcomeMax,
                    outcomePercentage
            ));
        }

        List<Evaluation> submitted = group.getSubmittedEvaluations();
        int submittedCount = submitted.size();
        Set<Long> submittedEvaluatorIds = submitted.stream()
                .map(e -> e.getEvaluator().getId())
                .collect(Collectors.toSet());

        int pendingCount = (group.getTeacher() != null && submittedEvaluatorIds.contains(group.getTeacher().getId())) ? 0 : 1;
        pendingCount += (int) group.getEvaluators().stream()
                .filter(ext -> !submittedEvaluatorIds.contains(ext.getId()))
                .count();

        return new IndividualReportData(
                getGeneratedAt(),
                new IndividualReportData.PeriodSummary(getPeriod().getId(), getPeriod().getCode()),
                new IndividualReportData.ProgramSummary(
                        moment.getProgram().getId(),
                        moment.getProgram().getCode(),
                        moment.getProgram().getName()),
                new IndividualReportData.MomentSummary(
                        moment.getId(),
                        moment.getNumber(),
                        moment.getName(),
                        moment.getSubject(),
                        maxScore),
                new IndividualReportData.StudentSummary(
                        student.getId(),
                        student.getStudentCode(),
                        student.getFullName(),
                        student.getCurrentSemester()),
                new IndividualReportData.GroupSummary(
                        group.getId(),
                        group.getName(),
                        group.getProjectTitle(),
                        group.getTeacher() != null ? group.getTeacher().getFullName() : null),
                finalResults,
                individualGrade,
                groupGrade,
                percentage,
                outcomes,
                submittedCount,
                pendingCount
        );
    }
}
