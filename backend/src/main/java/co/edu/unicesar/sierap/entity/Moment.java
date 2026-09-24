package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Assessment stage (momento) of a {@link Program} in an {@link AcademicPeriod},
 * e.g. "Momento 1" of Ingeniería de Sistemas in 2026-2.
 *
 * <p>It has a lifecycle (ADR-012): in {@link MomentStatus#CONFIGURATION} the admin
 * defines its {@link LearningOutcome}s, criteria, levels, members per group and
 * date windows, and students join groups; once launched
 * ({@link MomentStatus#LAUNCHED}) the configuration is locked and the windows tell
 * when work is submitted and when it is graded. The windows may be empty while the
 * moment is being configured.</p>
 *
 * <p>It owns its learning outcomes (composition) and is associated with a period,
 * which exists independently of the moment.</p>
 */
@Entity
@Table(name = "moment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "period_id", "number"}))
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Program that owns the moment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** Academic period in which the moment is configured. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    /** Number of the moment within the program and the period (1, 2, 3...). */
    @Column(nullable = false)
    private int number;

    /** Display name of the moment, e.g. "Momento 1". */
    @Column(nullable = false, length = 100)
    private String name;

    /** Course in which the moment is assessed. */
    @Column(nullable = false, length = 150)
    private String subject;

    /** First semester covered by the moment. */
    @Column(nullable = false)
    private int semesterFrom;

    /** Last semester covered by the moment. */
    @Column(nullable = false)
    private int semesterTo;

    /** Requirement level as free text (ADR-010), e.g. "Bajo". */
    @Column(nullable = false, length = 100)
    private String requirementLevel;

    /** Minimum number of students per group. */
    @Column(nullable = false)
    private int minMembers;

    /** Maximum number of students per group. */
    @Column(nullable = false)
    private int maxMembers;

    /** Lifecycle status; every new moment starts in configuration. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentStatus status = MomentStatus.CONFIGURATION;

    /** First day of the group formation window. */
    private LocalDate groupFormationStart;

    /** Last day of the group formation window. */
    private LocalDate groupFormationEnd;

    /** First day of the submission window. */
    private LocalDate submissionStart;

    /** Last day of the submission window. */
    private LocalDate submissionEnd;

    /** First day of the grading window. */
    private LocalDate gradingStart;

    /** Last day of the grading window. */
    private LocalDate gradingEnd;

    /** Learning outcomes assessed in the moment. */
    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code")
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();

    /**
     * Creates an empty moment in {@link MomentStatus#CONFIGURATION}. Its data is
     * filled in with the setters and it is linked to its program with
     * {@link Program#addMoment(Moment)}.
     */
    public Moment() {
    }

    /**
     * Tells whether groups can be formed today: the moment is still in
     * configuration and the date falls inside the group formation window.
     *
     * @param today current date, taken from the {@code Clock} injected in the service
     * @return {@code true} if the moment is in configuration and inside the window
     */
    public boolean isInGroupFormation(LocalDate today) {
        return status == MomentStatus.CONFIGURATION
                && isWithin(groupFormationStart, groupFormationEnd, today);
    }

    /**
     * Tells whether work can be submitted today: the moment is launched and the date
     * falls inside the submission window.
     *
     * @param today current date, taken from the {@code Clock} injected in the service
     * @return {@code true} if the moment is launched and inside the window
     */
    public boolean isInSubmission(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(submissionStart, submissionEnd, today);
    }

    /**
     * Tells whether grading is allowed today: the moment is launched and the date
     * falls inside the grading window.
     *
     * @param today current date, taken from the {@code Clock} injected in the service
     * @return {@code true} if the moment is launched and inside the window
     */
    public boolean isInGrading(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(gradingStart, gradingEnd, today);
    }

    /**
     * Maximum score that can be obtained in the moment: the sum of the maximum
     * scores of its learning outcomes.
     *
     * @return sum of {@link LearningOutcome#getMaxScore()}; zero if it has none
     */
    public BigDecimal getMaxScore() {
        return learningOutcomes.stream()
                .map(LearningOutcome::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adds a learning outcome to the moment and sets its moment.
     *
     * @param learningOutcome learning outcome to add
     */
    public void addLearningOutcome(LearningOutcome learningOutcome) {
        learningOutcomes.add(learningOutcome);
        learningOutcome.setMoment(this);
    }

    /**
     * Removes a learning outcome from the moment; on save, it is deleted together
     * with its criteria.
     *
     * @param learningOutcome learning outcome to remove
     */
    public void removeLearningOutcome(LearningOutcome learningOutcome) {
        learningOutcomes.remove(learningOutcome);
        learningOutcome.setMoment(null);
    }

    /**
     * Tells whether a date falls inside a window, both ends included. A window
     * without dates contains no date.
     */
    private static boolean isWithin(LocalDate start, LocalDate end, LocalDate date) {
        return start != null && end != null
                && !date.isBefore(start) && !date.isAfter(end);
    }

    /** @return identifier of the moment */
    public Long getId() {
        return id;
    }

    /** @return program that owns the moment */
    public Program getProgram() {
        return program;
    }

    /**
     * Sets the program of the moment. Use {@link Program#addMoment(Moment)} to keep
     * both sides of the relationship in sync.
     *
     * @param program program that owns the moment
     */
    void setProgram(Program program) {
        this.program = program;
    }

    /** @return academic period of the moment */
    public AcademicPeriod getPeriod() {
        return period;
    }

    /** @param period academic period in which the moment is configured */
    public void setPeriod(AcademicPeriod period) {
        this.period = period;
    }

    /** @return number of the moment */
    public int getNumber() {
        return number;
    }

    /** @param number new number of the moment */
    public void setNumber(int number) {
        this.number = number;
    }

    /** @return display name of the moment */
    public String getName() {
        return name;
    }

    /** @param name new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return course in which it is assessed */
    public String getSubject() {
        return subject;
    }

    /** @param subject new course */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /** @return first semester covered */
    public int getSemesterFrom() {
        return semesterFrom;
    }

    /** @param semesterFrom new first semester */
    public void setSemesterFrom(int semesterFrom) {
        this.semesterFrom = semesterFrom;
    }

    /** @return last semester covered */
    public int getSemesterTo() {
        return semesterTo;
    }

    /** @param semesterTo new last semester */
    public void setSemesterTo(int semesterTo) {
        this.semesterTo = semesterTo;
    }

    /** @return requirement level as free text */
    public String getRequirementLevel() {
        return requirementLevel;
    }

    /** @param requirementLevel new requirement level */
    public void setRequirementLevel(String requirementLevel) {
        this.requirementLevel = requirementLevel;
    }

    /** @return minimum number of students per group */
    public int getMinMembers() {
        return minMembers;
    }

    /** @param minMembers new minimum number of students per group */
    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    /** @return maximum number of students per group */
    public int getMaxMembers() {
        return maxMembers;
    }

    /** @param maxMembers new maximum number of students per group */
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    /** @return lifecycle status */
    public MomentStatus getStatus() {
        return status;
    }

    /** @param status new lifecycle status */
    public void setStatus(MomentStatus status) {
        this.status = status;
    }

    /** @return first day of the group formation window, or {@code null} */
    public LocalDate getGroupFormationStart() {
        return groupFormationStart;
    }

    /** @param groupFormationStart first day of the group formation window */
    public void setGroupFormationStart(LocalDate groupFormationStart) {
        this.groupFormationStart = groupFormationStart;
    }

    /** @return last day of the group formation window, or {@code null} */
    public LocalDate getGroupFormationEnd() {
        return groupFormationEnd;
    }

    /** @param groupFormationEnd last day of the group formation window */
    public void setGroupFormationEnd(LocalDate groupFormationEnd) {
        this.groupFormationEnd = groupFormationEnd;
    }

    /** @return first day of the submission window, or {@code null} */
    public LocalDate getSubmissionStart() {
        return submissionStart;
    }

    /** @param submissionStart first day of the submission window */
    public void setSubmissionStart(LocalDate submissionStart) {
        this.submissionStart = submissionStart;
    }

    /** @return last day of the submission window, or {@code null} */
    public LocalDate getSubmissionEnd() {
        return submissionEnd;
    }

    /** @param submissionEnd last day of the submission window */
    public void setSubmissionEnd(LocalDate submissionEnd) {
        this.submissionEnd = submissionEnd;
    }

    /** @return first day of the grading window, or {@code null} */
    public LocalDate getGradingStart() {
        return gradingStart;
    }

    /** @param gradingStart first day of the grading window */
    public void setGradingStart(LocalDate gradingStart) {
        this.gradingStart = gradingStart;
    }

    /** @return last day of the grading window, or {@code null} */
    public LocalDate getGradingEnd() {
        return gradingEnd;
    }

    /** @param gradingEnd last day of the grading window */
    public void setGradingEnd(LocalDate gradingEnd) {
        this.gradingEnd = gradingEnd;
    }

    /** @return learning outcomes of the moment, read-only (use add/remove) */
    public List<LearningOutcome> getLearningOutcomes() {
        return Collections.unmodifiableList(learningOutcomes);
    }
}
