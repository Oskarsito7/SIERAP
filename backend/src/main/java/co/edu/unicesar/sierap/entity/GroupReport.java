package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.GroupReportData;

/**
 * Reporte por grupo de proyecto de aula (Etapa 7).
 */
public class GroupReport extends Report {

    private final ProjectGroup group;
    private final LocalDate today;

    /**
     * Construye un reporte por grupo.
     *
     * @param generatedAt marca de tiempo de generación
     * @param group       grupo evaluado
     * @param today       fecha actual para verificar si cerró la ventana de calificación
     */
    public GroupReport(LocalDateTime generatedAt, ProjectGroup group, LocalDate today) {
        super(generatedAt, group.getMoment().getPeriod());
        this.group = Objects.requireNonNull(group, "group no puede ser nulo");
        this.today = Objects.requireNonNull(today, "today no puede ser nulo");
    }

    /**
     * Grupo evaluado en este reporte.
     *
     * @return grupo
     */
    public ProjectGroup getGroup() {
        return group;
    }

    @Override
    public GroupReportData generate() {
        Moment moment = group.getMoment();
        boolean finalResults = moment.isGradingClosed(today);

        BigDecimal groupGrade = group.getGroupGrade().orElse(null);
        BigDecimal maxScore = moment.getMaxScore();

        BigDecimal percentage = null;
        if (groupGrade != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            percentage = groupGrade.multiply(BigDecimal.valueOf(100))
                    .divide(maxScore, 2, RoundingMode.HALF_UP);
        }

        List<GroupReportData.OutcomeScore> outcomes = new ArrayList<>();
        for (LearningOutcome outcome : moment.getLearningOutcomes()) {
            BigDecimal grpScore = group.getGroupScoreFor(outcome).orElse(null);
            BigDecimal outcomeMax = outcome.getMaxScore();
            BigDecimal outcomePercentage = null;
            if (grpScore != null && outcomeMax != null && outcomeMax.compareTo(BigDecimal.ZERO) > 0) {
                outcomePercentage = grpScore.multiply(BigDecimal.valueOf(100))
                        .divide(outcomeMax, 2, RoundingMode.HALF_UP);
            }
            outcomes.add(new GroupReportData.OutcomeScore(
                    outcome.getId(),
                    outcome.getCode(),
                    outcome.getDescription(),
                    grpScore,
                    outcomeMax,
                    outcomePercentage
            ));
        }

        List<GroupReportData.MemberSummary> members = new ArrayList<>();
        List<Student> sortedStudents = group.getMembers().stream()
                .sorted(Comparator.comparing(Student::getFullName))
                .toList();

        for (Student student : sortedStudents) {
            BigDecimal indGrade = group.getIndividualGrade(student).orElse(null);
            boolean adjusted = false;
            if (groupGrade != null && indGrade != null && groupGrade.compareTo(indGrade) != 0) {
                adjusted = true;
            }

            List<GroupReportData.MemberOutcomeScore> memberOutcomes = new ArrayList<>();
            for (LearningOutcome outcome : moment.getLearningOutcomes()) {
                BigDecimal memberScore = group.getIndividualScoreFor(outcome, student).orElse(null);
                BigDecimal groupScore = group.getGroupScoreFor(outcome).orElse(null);
                if (groupScore != null && memberScore != null && groupScore.compareTo(memberScore) != 0) {
                    adjusted = true;
                }
                BigDecimal outcomeMax = outcome.getMaxScore();
                BigDecimal mPercentage = null;
                if (memberScore != null && outcomeMax != null && outcomeMax.compareTo(BigDecimal.ZERO) > 0) {
                    mPercentage = memberScore.multiply(BigDecimal.valueOf(100))
                            .divide(outcomeMax, 2, RoundingMode.HALF_UP);
                }
                memberOutcomes.add(new GroupReportData.MemberOutcomeScore(
                        outcome.getId(),
                        outcome.getCode(),
                        memberScore,
                        outcomeMax,
                        mPercentage
                ));
            }

            members.add(new GroupReportData.MemberSummary(
                    student.getId(),
                    student.getStudentCode(),
                    student.getFullName(),
                    indGrade,
                    adjusted,
                    memberOutcomes
            ));
        }

        List<Evaluation> submitted = group.getSubmittedEvaluations();
        int submittedCount = submitted.size();
        int pendingCount = group.getPendingEvaluationsCount();

        return new GroupReportData(
                getGeneratedAt(),
                new GroupReportData.PeriodSummary(getPeriod().getId(), getPeriod().getCode()),
                new GroupReportData.ProgramSummary(
                        moment.getProgram().getId(),
                        moment.getProgram().getCode(),
                        moment.getProgram().getName()),
                new GroupReportData.MomentSummary(
                        moment.getId(),
                        moment.getNumber(),
                        moment.getName(),
                        moment.getSubject(),
                        maxScore),
                new GroupReportData.GroupSummary(
                        group.getId(),
                        group.getName(),
                        group.getProjectTitle(),
                        group.getTeacher() != null ? group.getTeacher().getFullName() : null),
                finalResults,
                groupGrade,
                percentage,
                outcomes,
                members,
                submittedCount,
                pendingCount
        );
    }
}
