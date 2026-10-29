package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Grupo y proyecto de aula a la vez (cada grupo tiene un único proyecto).
 *
 * <p>Un {@link Teacher} lo habilita en un {@link Moment}; los estudiantes del
 * programa se inscriben en él durante la ventana de conformación, entre el mínimo y
 * el máximo de integrantes del momento. El docente le asigna evaluadores externos y
 * fija sus fechas de entrega y de evaluación dentro de las ventanas del momento
 * (RF 7 y 9, ADR-012).</p>
 *
 * <p>Relaciones: composición con {@link Submission} y {@link Evaluation} (se borran
 * con el grupo); agregación con {@link Student} (el estudiante existe sin el grupo);
 * asociación con {@link Teacher} y {@link ExternalEvaluator}.</p>
 *
 * <p>La entidad solo responde preguntas ({@code isFull()}, {@code hasEvaluator()}…);
 * los services las usan para decidir y lanzar {@code BusinessRuleException} o
 * {@code ForbiddenAccessException}.</p>
 */
@Entity
@Table(name = "project_group")
public class ProjectGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Momento en el que el docente habilitó el grupo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id", nullable = false)
    private Moment moment;

    /** Docente que habilitó el grupo; es el único docente que lo califica. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    /** Nombre visible del grupo, p. ej. "Grupo 1 - Diurno". */
    @Column(nullable = false, length = 100)
    private String name;

    /** Título del proyecto de aula; puede definirse después de conformar el grupo. */
    @Column(length = 200)
    private String projectTitle;

    /** Fecha límite de entrega fijada por el docente dentro de la ventana de entregas. */
    private LocalDate submissionDeadline;

    /** Fecha de evaluación fijada por el docente dentro de la ventana de calificación. */
    private LocalDate evaluationDate;

    /** Estudiantes inscritos. */
    @ManyToMany
    @JoinTable(name = "project_group_member",
            joinColumns = @JoinColumn(name = "project_group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<Student> members = new LinkedHashSet<>();

    /** Evaluadores externos asignados por el docente. */
    @ManyToMany
    @JoinTable(name = "project_group_evaluator",
            joinColumns = @JoinColumn(name = "project_group_id"),
            inverseJoinColumns = @JoinColumn(name = "external_evaluator_id"))
    private Set<ExternalEvaluator> evaluators = new LinkedHashSet<>();

    /** Versiones de la entrega, de la más antigua a la más reciente. */
    @OneToMany(mappedBy = "projectGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber")
    private List<Submission> submissions = new ArrayList<>();

    /** Evaluaciones recibidas (una por evaluador). */
    @OneToMany(mappedBy = "projectGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evaluation> evaluations = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected ProjectGroup() {
    }

    /**
     * Crea un grupo habilitado por un docente en un momento.
     *
     * @param moment  momento en el que se habilita
     * @param teacher docente que lo habilita
     * @param name    nombre visible del grupo
     */
    public ProjectGroup(Moment moment, Teacher teacher, String name) {
        this.moment = moment;
        this.teacher = teacher;
        this.name = name;
    }

    /**
     * Indica si el grupo ya alcanzó el máximo de integrantes del momento.
     *
     * @return {@code true} si no admite más estudiantes
     */
    public boolean isFull() {
        return members.size() >= moment.getMaxMembers();
    }

    /**
     * Indica si el grupo tiene al menos el mínimo de integrantes del momento.
     *
     * @return {@code true} si cumple el mínimo
     */
    public boolean hasMinimumMembers() {
        return members.size() >= moment.getMinMembers();
    }

    /**
     * Indica si un estudiante está inscrito en el grupo.
     *
     * @param student estudiante a consultar
     * @return {@code true} si es integrante
     */
    public boolean isMember(Student student) {
        return members.stream().anyMatch(member -> sameUser(member, student));
    }

    /**
     * Inscribe a un estudiante. Las reglas (ventana de conformación, cupo, un grupo
     * por momento) las valida el service antes de llamar este método.
     *
     * @param student estudiante que se inscribe
     */
    public void addMember(Student student) {
        members.add(student);
    }

    /**
     * Retira a un estudiante del grupo.
     *
     * @param student estudiante que sale
     */
    public void removeMember(Student student) {
        members.removeIf(member -> sameUser(member, student));
    }

    /**
     * Asigna un evaluador externo al grupo.
     *
     * @param evaluator evaluador externo asignado por el docente
     */
    public void addEvaluator(ExternalEvaluator evaluator) {
        evaluators.add(evaluator);
    }

    /**
     * Quita la asignación de un evaluador externo.
     *
     * @param evaluator evaluador externo a retirar
     */
    public void removeEvaluator(ExternalEvaluator evaluator) {
        evaluators.removeIf(assigned -> sameUser(assigned, evaluator));
    }

    /**
     * Indica si el grupo lo habilitó un docente. Es la regla que usa
     * {@link Teacher#canEvaluate(ProjectGroup)}. Compara por id y no con
     * {@code equals}: el docente se carga LAZY y Hibernate puede entregar un proxy,
     * que no es {@code equals} al objeto real aunque representen la misma fila.
     *
     * @param candidate docente a consultar
     * @return {@code true} si es el docente que habilitó el grupo
     */
    public boolean isEnabledBy(Teacher candidate) {
        return sameUser(teacher, candidate);
    }

    /**
     * Indica si un evaluador externo está designado para el grupo. Es la regla que
     * usa {@link ExternalEvaluator#canEvaluate(ProjectGroup)}.
     *
     * @param candidate evaluador externo a consultar
     * @return {@code true} si está entre los evaluadores designados
     */
    public boolean hasEvaluator(ExternalEvaluator candidate) {
        return evaluators.stream().anyMatch(evaluator -> sameUser(evaluator, candidate));
    }

    /**
     * Número que le corresponde a la próxima versión de la entrega.
     *
     * @return 1 si aún no hay entregas; si no, la última versión + 1
     */
    public int nextSubmissionVersion() {
        return submissions.isEmpty()
                ? 1
                : submissions.get(submissions.size() - 1).getVersionNumber() + 1;
    }

    /**
     * Versión de la entrega que se califica: la última entregada a tiempo; si no hay
     * ninguna a tiempo, la última tardía.
     *
     * @return la entrega que se califica, o {@code null} si el grupo no ha entregado
     */
    public Submission getSubmissionToGrade() {
        Submission lastLate = null;
        for (int index = submissions.size() - 1; index >= 0; index--) {
            Submission submission = submissions.get(index);
            if (!submission.isLate()) {
                return submission;
            }
            if (lastLate == null) {
                lastLate = submission;
            }
        }
        return lastLate;
    }

    /**
     * Agrega una nueva versión de la entrega y fija el grupo de la entrega.
     *
     * @param submission entrega creada con {@link #nextSubmissionVersion()}
     */
    public void addSubmission(Submission submission) {
        submissions.add(submission);
        submission.setProjectGroup(this);
    }

    /**
     * Registra la evaluación de un evaluador y fija el grupo de la evaluación.
     *
     * @param evaluation evaluación del docente o de un evaluador externo
     */
    public void addEvaluation(Evaluation evaluation) {
        evaluations.add(evaluation);
        evaluation.setProjectGroup(this);
    }

    /**
     * Evaluaciones enviadas del grupo: las únicas que cuentan en las notas.
     *
     * @return evaluaciones en estado {@code SUBMITTED}
     */
    public List<Evaluation> getSubmittedEvaluations() {
        return evaluations.stream().filter(Evaluation::isSubmitted).toList();
    }

    /**
     * Nota del grupo (ADR-018): promedio ponderado por tipo de evaluador de las
     * evaluaciones enviadas, con los niveles del grupo y los pesos del momento.
     *
     * @return la nota con 2 decimales, o vacío si ninguna evaluación enviada cuenta
     */
    public Optional<BigDecimal> getGroupGrade() {
        return weightedGrade(Evaluation::getTotalScore);
    }

    /**
     * Puntaje del grupo en un RAP, con la misma fórmula que {@link #getGroupGrade()}.
     *
     * @param outcome RAP del momento
     * @return el puntaje con 2 decimales, o vacío si ninguna evaluación enviada cuenta
     */
    public Optional<BigDecimal> getGroupScoreFor(LearningOutcome outcome) {
        return weightedGrade(evaluation -> evaluation.getScoreFor(outcome));
    }

    /**
     * Nota individual de un integrante, la definitiva (ADR-018): la misma fórmula, con
     * los niveles del grupo cambiados por sus ajustes. Sin ajustes es igual a la del grupo.
     *
     * @param student integrante del grupo
     * @return la nota con 2 decimales, o vacío si ninguna evaluación enviada cuenta
     */
    public Optional<BigDecimal> getIndividualGrade(Student student) {
        return weightedGrade(evaluation -> evaluation.getTotalScoreFor(student));
    }

    /**
     * Puntaje de un integrante en un RAP, con sus ajustes.
     *
     * @param outcome RAP del momento
     * @param student integrante del grupo
     * @return el puntaje con 2 decimales, o vacío si ninguna evaluación enviada cuenta
     */
    public Optional<BigDecimal> getIndividualScoreFor(LearningOutcome outcome, Student student) {
        return weightedGrade(evaluation -> evaluation.getScoreFor(outcome, student));
    }

    /**
     * Aplica la fórmula de {@link GradeCalculator} a las evaluaciones enviadas: el
     * puntaje de cada una con el tipo de su evaluador y los pesos del momento.
     */
    private Optional<BigDecimal> weightedGrade(Function<Evaluation, BigDecimal> score) {
        List<GradeCalculator.KindScore> scores = new ArrayList<>();
        for (Evaluation evaluation : getSubmittedEvaluations()) {
            evaluatorKindOf(evaluation).ifPresent(kind ->
                    scores.add(new GradeCalculator.KindScore(kind, score.apply(evaluation))));
        }
        Map<GradeCalculator.EvaluatorKind, BigDecimal> weights = new HashMap<>();
        weights.put(GradeCalculator.EvaluatorKind.TEACHER, moment.getTeacherWeight());
        moment.getEvaluatorWeights().forEach(weight -> weights.put(
                GradeCalculator.EvaluatorKind.ofType(weight.getEvaluatorType().getId()), weight.getWeight()));
        return GradeCalculator.weightedGrade(scores, weights);
    }

    /**
     * Tipo de evaluador de una evaluación del grupo: el docente que lo habilitó o el tipo
     * de uno de sus evaluadores externos. Se busca por id entre los evaluadores del grupo
     * para no depender del proxy con que Hibernate carga {@link Evaluation#getEvaluator()}.
     */
    private Optional<GradeCalculator.EvaluatorKind> evaluatorKindOf(Evaluation evaluation) {
        User evaluator = evaluation.getEvaluator();
        if (sameUser(evaluator, teacher)) {
            return Optional.of(GradeCalculator.EvaluatorKind.TEACHER);
        }
        return evaluators.stream()
                .filter(external -> sameUser(external, evaluator))
                .findFirst()
                .map(external -> GradeCalculator.EvaluatorKind.ofType(external.getEvaluatorType().getId()));
    }

    /** Dos usuarios son el mismo si son la misma referencia o tienen el mismo id. */
    private static boolean sameUser(User first, User second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    public Long getId() {
        return id;
    }

    public Moment getMoment() {
        return moment;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public LocalDate getSubmissionDeadline() {
        return submissionDeadline;
    }

    public void setSubmissionDeadline(LocalDate submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    public LocalDate getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDate evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public Set<Student> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<ExternalEvaluator> getEvaluators() {
        return Collections.unmodifiableSet(evaluators);
    }

    public List<Submission> getSubmissions() {
        return Collections.unmodifiableList(submissions);
    }

    public List<Evaluation> getEvaluations() {
        return Collections.unmodifiableList(evaluations);
    }
}
