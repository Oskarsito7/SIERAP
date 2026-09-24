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
 * Momento: etapa de evaluación de un {@link Program} en un {@link AcademicPeriod}
 * (p. ej. "Momento 1" de Ingeniería de Sistemas en 2026-2).
 *
 * <p>Tiene un ciclo de vida (ADR-012): en {@link MomentStatus#CONFIGURATION} el
 * admin define sus RAP ({@link LearningOutcome}), criterios, niveles, integrantes
 * por grupo y ventanas de fechas, y los estudiantes se inscriben en grupos; al
 * lanzarlo ({@link MomentStatus#LAUNCHED}) la configuración queda bloqueada y las
 * ventanas indican cuándo se entrega y cuándo se califica. Las ventanas pueden
 * estar vacías mientras el momento se configura.</p>
 *
 * <p>Es dueño de sus RAP (composición) y se asocia a un periodo, que existe
 * independientemente del momento.</p>
 */
@Entity
@Table(name = "moment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "period_id", "number"}))
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Programa dueño del momento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** Periodo académico en el que se configura el momento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    /** Número del momento dentro del programa y el periodo (1, 2, 3...). */
    @Column(nullable = false)
    private int number;

    /** Nombre visible del momento, p. ej. "Momento 1". */
    @Column(nullable = false, length = 100)
    private String name;

    /** Asignatura en la que se evalúa el momento. */
    @Column(nullable = false, length = 150)
    private String subject;

    /** Primer semestre que abarca el momento. */
    @Column(nullable = false)
    private int semesterFrom;

    /** Último semestre que abarca el momento. */
    @Column(nullable = false)
    private int semesterTo;

    /** Nivel de exigencia en texto libre (ADR-010), p. ej. "Bajo". */
    @Column(nullable = false, length = 100)
    private String requirementLevel;

    /** Mínimo de estudiantes por grupo. */
    @Column(nullable = false)
    private int minMembers;

    /** Máximo de estudiantes por grupo. */
    @Column(nullable = false)
    private int maxMembers;

    /** Estado del ciclo de vida; todo momento nuevo empieza en configuración. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentStatus status = MomentStatus.CONFIGURATION;

    /** Primer día de la ventana de conformación de grupos. */
    private LocalDate groupFormationStart;

    /** Último día de la ventana de conformación de grupos. */
    private LocalDate groupFormationEnd;

    /** Primer día de la ventana de entregas. */
    private LocalDate submissionStart;

    /** Último día de la ventana de entregas. */
    private LocalDate submissionEnd;

    /** Primer día de la ventana de calificación. */
    private LocalDate gradingStart;

    /** Último día de la ventana de calificación. */
    private LocalDate gradingEnd;

    /** RAP evaluados en el momento. */
    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code")
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();

    /**
     * Crea un momento vacío en estado {@link MomentStatus#CONFIGURATION}. Sus datos
     * se completan con los setters y se asocia a su programa con
     * {@link Program#addMoment(Moment)}.
     */
    public Moment() {
    }

    /**
     * Indica si hoy se pueden conformar grupos: el momento sigue en configuración y
     * la fecha cae dentro de la ventana de conformación.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está en configuración y dentro de la ventana
     */
    public boolean isInGroupFormation(LocalDate today) {
        return status == MomentStatus.CONFIGURATION
                && isWithin(groupFormationStart, groupFormationEnd, today);
    }

    /**
     * Indica si hoy se pueden subir entregas: el momento está lanzado y la fecha cae
     * dentro de la ventana de entregas.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está lanzado y dentro de la ventana
     */
    public boolean isInSubmission(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(submissionStart, submissionEnd, today);
    }

    /**
     * Indica si hoy se puede calificar: el momento está lanzado y la fecha cae dentro
     * de la ventana de calificación.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está lanzado y dentro de la ventana
     */
    public boolean isInGrading(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(gradingStart, gradingEnd, today);
    }

    /**
     * Puntaje máximo que se puede obtener en el momento: la suma de los puntajes
     * máximos de sus RAP.
     *
     * @return suma de {@link LearningOutcome#getMaxScore()}; cero si no tiene RAP
     */
    public BigDecimal getMaxScore() {
        return learningOutcomes.stream()
                .map(LearningOutcome::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un RAP al momento y fija el momento del RAP.
     *
     * @param learningOutcome RAP a agregar
     */
    public void addLearningOutcome(LearningOutcome learningOutcome) {
        learningOutcomes.add(learningOutcome);
        learningOutcome.setMoment(this);
    }

    /**
     * Quita un RAP del momento; al guardar, el RAP se elimina con sus criterios.
     *
     * @param learningOutcome RAP a quitar
     */
    public void removeLearningOutcome(LearningOutcome learningOutcome) {
        learningOutcomes.remove(learningOutcome);
        learningOutcome.setMoment(null);
    }

    /**
     * Indica si una fecha cae dentro de una ventana, incluyendo sus extremos. Una
     * ventana sin fechas definidas no contiene ninguna fecha.
     */
    private static boolean isWithin(LocalDate start, LocalDate end, LocalDate date) {
        return start != null && end != null
                && !date.isBefore(start) && !date.isAfter(end);
    }

    /** @return identificador del momento */
    public Long getId() {
        return id;
    }

    /** @return programa dueño del momento */
    public Program getProgram() {
        return program;
    }

    /**
     * Fija el programa del momento. Usar {@link Program#addMoment(Moment)} para
     * mantener sincronizados los dos lados de la relación.
     *
     * @param program programa dueño del momento
     */
    void setProgram(Program program) {
        this.program = program;
    }

    /** @return periodo académico del momento */
    public AcademicPeriod getPeriod() {
        return period;
    }

    /** @param period periodo académico en el que se configura el momento */
    public void setPeriod(AcademicPeriod period) {
        this.period = period;
    }

    /** @return número del momento */
    public int getNumber() {
        return number;
    }

    /** @param number nuevo número del momento */
    public void setNumber(int number) {
        this.number = number;
    }

    /** @return nombre visible del momento */
    public String getName() {
        return name;
    }

    /** @param name nuevo nombre visible */
    public void setName(String name) {
        this.name = name;
    }

    /** @return asignatura en la que se evalúa */
    public String getSubject() {
        return subject;
    }

    /** @param subject nueva asignatura */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /** @return primer semestre que abarca */
    public int getSemesterFrom() {
        return semesterFrom;
    }

    /** @param semesterFrom nuevo primer semestre */
    public void setSemesterFrom(int semesterFrom) {
        this.semesterFrom = semesterFrom;
    }

    /** @return último semestre que abarca */
    public int getSemesterTo() {
        return semesterTo;
    }

    /** @param semesterTo nuevo último semestre */
    public void setSemesterTo(int semesterTo) {
        this.semesterTo = semesterTo;
    }

    /** @return nivel de exigencia en texto libre */
    public String getRequirementLevel() {
        return requirementLevel;
    }

    /** @param requirementLevel nuevo nivel de exigencia */
    public void setRequirementLevel(String requirementLevel) {
        this.requirementLevel = requirementLevel;
    }

    /** @return mínimo de estudiantes por grupo */
    public int getMinMembers() {
        return minMembers;
    }

    /** @param minMembers nuevo mínimo de estudiantes por grupo */
    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    /** @return máximo de estudiantes por grupo */
    public int getMaxMembers() {
        return maxMembers;
    }

    /** @param maxMembers nuevo máximo de estudiantes por grupo */
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    /** @return estado del ciclo de vida */
    public MomentStatus getStatus() {
        return status;
    }

    /** @param status nuevo estado del ciclo de vida */
    public void setStatus(MomentStatus status) {
        this.status = status;
    }

    /** @return primer día de la ventana de conformación, o {@code null} */
    public LocalDate getGroupFormationStart() {
        return groupFormationStart;
    }

    /** @param groupFormationStart primer día de la ventana de conformación */
    public void setGroupFormationStart(LocalDate groupFormationStart) {
        this.groupFormationStart = groupFormationStart;
    }

    /** @return último día de la ventana de conformación, o {@code null} */
    public LocalDate getGroupFormationEnd() {
        return groupFormationEnd;
    }

    /** @param groupFormationEnd último día de la ventana de conformación */
    public void setGroupFormationEnd(LocalDate groupFormationEnd) {
        this.groupFormationEnd = groupFormationEnd;
    }

    /** @return primer día de la ventana de entregas, o {@code null} */
    public LocalDate getSubmissionStart() {
        return submissionStart;
    }

    /** @param submissionStart primer día de la ventana de entregas */
    public void setSubmissionStart(LocalDate submissionStart) {
        this.submissionStart = submissionStart;
    }

    /** @return último día de la ventana de entregas, o {@code null} */
    public LocalDate getSubmissionEnd() {
        return submissionEnd;
    }

    /** @param submissionEnd último día de la ventana de entregas */
    public void setSubmissionEnd(LocalDate submissionEnd) {
        this.submissionEnd = submissionEnd;
    }

    /** @return primer día de la ventana de calificación, o {@code null} */
    public LocalDate getGradingStart() {
        return gradingStart;
    }

    /** @param gradingStart primer día de la ventana de calificación */
    public void setGradingStart(LocalDate gradingStart) {
        this.gradingStart = gradingStart;
    }

    /** @return último día de la ventana de calificación, o {@code null} */
    public LocalDate getGradingEnd() {
        return gradingEnd;
    }

    /** @param gradingEnd último día de la ventana de calificación */
    public void setGradingEnd(LocalDate gradingEnd) {
        this.gradingEnd = gradingEnd;
    }

    /** @return RAP del momento, en solo lectura (usar add/remove) */
    public List<LearningOutcome> getLearningOutcomes() {
        return Collections.unmodifiableList(learningOutcomes);
    }
}
