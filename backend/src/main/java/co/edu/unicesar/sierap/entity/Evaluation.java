package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Registra el nivel elegido en un criterio. Si el criterio ya tenía nivel, lo
     * cambia en la misma fila (el evaluador corrigió su elección): borrar y volver a
     * insertar violaría la restricción única evaluación + criterio, porque Hibernate
     * ejecuta los INSERT antes que los DELETE.
     *
     * @param level nivel de desempeño elegido; su criterio es el que se califica
     */
    public void grade(PerformanceLevel level) {
        EvaluationCriterion criterion = level.getCriterion();
        grades.stream()
                .filter(grade -> sameCriterion(grade.getCriterion(), criterion))
                .findFirst()
                .ifPresentOrElse(grade -> grade.changeLevel(level), () -> {
                    CriterionGrade grade = new CriterionGrade(level);
                    grades.add(grade);
                    grade.setEvaluation(this);
                });
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
}
