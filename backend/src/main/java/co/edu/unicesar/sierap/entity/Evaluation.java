package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Calificación que un evaluador (el docente del grupo o un evaluador externo
 * asignado) le da a un {@link ProjectGroup}.
 *
 * <p>Agrupa una {@link CriterionGrade} por cada criterio del momento. La nota de un
 * RAP es la suma de sus criterios y la nota de la evaluación, la suma de todos
 * (ADR-013: {@code BigDecimal}, sin redondeos). Cada evaluador califica una sola
 * vez cada grupo (restricción única grupo + evaluador); si corrige, se reemplaza la
 * calificación del criterio.</p>
 *
 * <p>Empieza como borrador ({@link EvaluationStatus#DRAFT}): el evaluador la guarda a
 * medias y la envía después con {@link #submit()}. Solo las enviadas cuentan para la
 * nota del grupo, y se pueden corregir mientras la ventana de calificación esté
 * abierta (lo valida el service con el {@code Clock}).</p>
 *
 * <p>El grupo se califica una sola vez y cada integrante hereda esos niveles; el
 * evaluador puede ajustar el nivel de algunos criterios para un estudiante con
 * {@link #adjust(Student, PerformanceLevel)} (ADR-018). Así salen dos notas: la del
 * grupo ({@link #getTotalScore()}) y la de cada estudiante
 * ({@link #getTotalScoreFor(Student)}), que usa sus ajustes donde los tenga.</p>
 *
 * <p>El evaluador se guarda como {@link User} porque JPA no puede mapear una
 * interfaz: la columna apunta a {@code app_user}. Que sea un {@link Evaluator} que
 * puede calificar el grupo lo valida el service con
 * {@link Evaluator#canEvaluate(ProjectGroup)}.</p>
 */
@Entity
@Table(name = "evaluation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_group_id", "evaluator_id"}))
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Grupo evaluado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_group_id", nullable = false)
    private ProjectGroup projectGroup;

    /** Quien califica: un {@link Teacher} o un {@link ExternalEvaluator}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    /** Borrador o enviada; toda evaluación nueva empieza como borrador. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    /** Fecha y hora de la última modificación, tomada del {@code Clock} de la aplicación. */
    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    /** Observación general del evaluador para el grupo; opcional. */
    @Column(length = 2000)
    private String comment;

    /** Nivel elegido en cada criterio. */
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterionGrade> grades = new ArrayList<>();

    /** Ajustes por estudiante: solo los criterios en que su nivel difiere del grupo. */
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndividualGrade> individualGrades = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Evaluation() {
    }

    /**
     * Crea una evaluación vacía. Se asocia al grupo con
     * {@link ProjectGroup#addEvaluation(Evaluation)}.
     *
     * @param evaluator   usuario que califica
     * @param evaluatedAt fecha y hora de la calificación
     */
    public Evaluation(User evaluator, LocalDateTime evaluatedAt) {
        this.evaluator = evaluator;
        this.evaluatedAt = evaluatedAt;
    }

    /**
     * Registra el nivel del grupo en un criterio. Si el criterio ya tenía nivel, lo
     * cambia en la misma fila (el evaluador corrigió su elección): borrar y volver a
     * insertar violaría la restricción única evaluación + criterio, porque Hibernate
     * ejecuta los INSERT antes que los DELETE.
     *
     * <p>Los ajustes individuales de ese criterio que quedan iguales al nuevo nivel
     * del grupo se eliminan: un ajuste solo existe si difiere del grupo.</p>
     *
     * @param level nivel de desempeño elegido; su criterio es el que se califica
     */
    public void grade(PerformanceLevel level) {
        EvaluationCriterion criterion = level.getCriterion();
        findGrade(criterion).ifPresentOrElse(grade -> grade.changeLevel(level), () -> {
            CriterionGrade grade = new CriterionGrade(level);
            grades.add(grade);
            grade.setEvaluation(this);
        });
        individualGrades.removeIf(adjustment -> sameCriterion(adjustment.getCriterion(), criterion)
                && sameLevel(adjustment.getLevel(), level));
    }

    /**
     * Ajusta el nivel de un criterio para un solo estudiante (ADR-018). Si ya tenía un
     * ajuste en ese criterio, lo cambia en la misma fila; si el nivel es igual al del
     * grupo, elimina el ajuste, porque el estudiante vuelve a heredar el nivel del grupo.
     *
     * @param student integrante del grupo evaluado
     * @param level   nivel para ese estudiante; su criterio es el que se ajusta
     * @throws IllegalArgumentException si el estudiante no es integrante del grupo
     *                                  (error de programación: el service debe
     *                                  validarlo antes y responder 422)
     */
    public void adjust(Student student, PerformanceLevel level) {
        if (!projectGroup.isMember(student)) {
            throw new IllegalArgumentException("El estudiante no es integrante del grupo evaluado");
        }
        EvaluationCriterion criterion = level.getCriterion();
        boolean sameAsGroup = findGrade(criterion).map(grade -> sameLevel(grade.getLevel(), level)).orElse(false);
        if (sameAsGroup) {
            removeAdjustment(student, criterion);
            return;
        }
        findAdjustment(student, criterion).ifPresentOrElse(adjustment -> adjustment.changeLevel(level), () -> {
            IndividualGrade adjustment = new IndividualGrade(student, level);
            individualGrades.add(adjustment);
            adjustment.setEvaluation(this);
        });
    }

    /**
     * Quita el ajuste de un estudiante en un criterio; desde ahí hereda el nivel del
     * grupo. Si no tenía ajuste, no hace nada.
     *
     * @param student   estudiante
     * @param criterion criterio
     */
    public void removeAdjustment(Student student, EvaluationCriterion criterion) {
        individualGrades.removeIf(adjustment -> sameStudent(adjustment.getStudent(), student)
                && sameCriterion(adjustment.getCriterion(), criterion));
    }

    /**
     * Ajustes de un estudiante en esta evaluación.
     *
     * @param student estudiante
     * @return sus ajustes; vacío si hereda todos los niveles del grupo
     */
    public List<IndividualGrade> getAdjustmentsFor(Student student) {
        return individualGrades.stream()
                .filter(adjustment -> sameStudent(adjustment.getStudent(), student))
                .toList();
    }

    /**
     * Envía la evaluación: desde ahora cuenta para la nota del grupo. Que esté
     * completa ({@link #isComplete()}) y dentro de la ventana de calificación lo
     * valida el service antes de llamar este método.
     */
    public void submit() {
        status = EvaluationStatus.SUBMITTED;
    }

    /**
     * Indica si la evaluación ya fue enviada y cuenta para la nota del grupo.
     *
     * @return {@code true} si su estado es {@link EvaluationStatus#SUBMITTED}
     */
    public boolean isSubmitted() {
        return status == EvaluationStatus.SUBMITTED;
    }

    /**
     * Nota total: la suma de los puntajes de todos los criterios calificados.
     *
     * @return suma de los puntajes; cero si aún no hay criterios calificados
     */
    public BigDecimal getTotalScore() {
        return grades.stream()
                .map(CriterionGrade::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Nota en un RAP: la suma de los puntajes de los criterios de ese RAP.
     *
     * @param learningOutcome RAP a consultar
     * @return suma de los puntajes de sus criterios; cero si no se calificó ninguno
     */
    public BigDecimal getScoreFor(LearningOutcome learningOutcome) {
        return grades.stream()
                .filter(grade -> sameOutcome(grade.getCriterion().getLearningOutcome(), learningOutcome))
                .map(CriterionGrade::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Nota total de un estudiante: la del grupo, pero con el puntaje de sus ajustes en
     * los criterios donde los tiene (ADR-018). Sin ajustes es igual a
     * {@link #getTotalScore()}.
     *
     * @param student integrante del grupo
     * @return suma de los puntajes que le corresponden
     */
    public BigDecimal getTotalScoreFor(Student student) {
        return scoresFor(student, null);
    }

    /**
     * Nota de un estudiante en un RAP: la suma de los criterios de ese RAP, con sus
     * ajustes donde los tiene.
     *
     * @param learningOutcome RAP a consultar
     * @param student         integrante del grupo
     * @return suma de los puntajes que le corresponden en ese RAP
     */
    public BigDecimal getScoreFor(LearningOutcome learningOutcome, Student student) {
        return scoresFor(student, learningOutcome);
    }

    /**
     * Indica si se calificaron todos los criterios de todos los RAP del momento.
     *
     * @return {@code true} si hay una calificación por cada criterio del momento
     */
    public boolean isComplete() {
        return projectGroup.getMoment().getLearningOutcomes().stream()
                .flatMap(outcome -> outcome.getCriteria().stream())
                .allMatch(criterion -> grades.stream()
                        .anyMatch(grade -> sameCriterion(grade.getCriterion(), criterion)));
    }

    /**
     * Suma, criterio por criterio, el puntaje del ajuste del estudiante si lo tiene o
     * el del grupo si no; con {@code outcome} solo cuenta los criterios de ese RAP. Un
     * ajuste en un criterio que el grupo aún no tiene calificado (borrador) también
     * cuenta.
     */
    private BigDecimal scoresFor(Student student, LearningOutcome outcome) {
        List<IndividualGrade> adjustments = getAdjustmentsFor(student);
        BigDecimal groupPart = grades.stream()
                .filter(grade -> outcome == null || sameOutcome(grade.getCriterion().getLearningOutcome(), outcome))
                .map(grade -> adjustments.stream()
                        .filter(adjustment -> sameCriterion(adjustment.getCriterion(), grade.getCriterion()))
                        .findFirst()
                        .map(IndividualGrade::getScore)
                        .orElse(grade.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adjustedOnly = adjustments.stream()
                .filter(adjustment -> findGrade(adjustment.getCriterion()).isEmpty())
                .filter(adjustment -> outcome == null
                        || sameOutcome(adjustment.getCriterion().getLearningOutcome(), outcome))
                .map(IndividualGrade::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return groupPart.add(adjustedOnly);
    }

    private Optional<CriterionGrade> findGrade(EvaluationCriterion criterion) {
        return grades.stream().filter(grade -> sameCriterion(grade.getCriterion(), criterion)).findFirst();
    }

    private Optional<IndividualGrade> findAdjustment(Student student, EvaluationCriterion criterion) {
        return individualGrades.stream()
                .filter(adjustment -> sameStudent(adjustment.getStudent(), student)
                        && sameCriterion(adjustment.getCriterion(), criterion))
                .findFirst();
    }

    /** Compara estudiantes por id cuando ya están guardados, o por referencia si no. */
    private static boolean sameStudent(Student first, Student second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    /** Compara niveles por id cuando ya están guardados, o por referencia si no. */
    private static boolean sameLevel(PerformanceLevel first, PerformanceLevel second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    /** Compara criterios por id cuando ya están guardados, o por referencia si no. */
    private static boolean sameCriterion(EvaluationCriterion first, EvaluationCriterion second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    /** Compara RAP por id cuando ya están guardados, o por referencia si no. */
    private static boolean sameOutcome(LearningOutcome first, LearningOutcome second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    public Long getId() {
        return id;
    }

    public ProjectGroup getProjectGroup() {
        return projectGroup;
    }

    void setProjectGroup(ProjectGroup projectGroup) {
        this.projectGroup = projectGroup;
    }

    public User getEvaluator() {
        return evaluator;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<CriterionGrade> getGrades() {
        return Collections.unmodifiableList(grades);
    }

    public List<IndividualGrade> getIndividualGrades() {
        return Collections.unmodifiableList(individualGrades);
    }
}
