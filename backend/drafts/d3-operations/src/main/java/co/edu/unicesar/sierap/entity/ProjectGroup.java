package co.edu.unicesar.sierap.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * <p>La entidad solo responde preguntas ({@code isFull()}, {@code canBeGradedBy()}…);
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
        return members.stream().anyMatch(member -> sameId(member, student));
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
        members.remove(student);
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
        evaluators.remove(evaluator);
    }

    /**
     * Indica si un usuario puede calificar el grupo: su docente o un evaluador
     * externo asignado. Es la regla de alcance "cada evaluador solo califica lo
     * que se le asignó". Compara por id y no con {@code equals}: el docente se
     * carga LAZY y Hibernate puede entregar un proxy, que no es {@code equals} al
     * objeto real aunque representen la misma fila.
     *
     * @param user usuario que intenta calificar
     * @return {@code true} si es el docente del grupo o un evaluador asignado
     */
    public boolean canBeGradedBy(User user) {
        return sameId(teacher, user)
                || evaluators.stream().anyMatch(evaluator -> sameId(evaluator, user));
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
     * Última versión entregada, que es la que se califica.
     *
     * @return la entrega más reciente, o {@code null} si el grupo no ha entregado
     */
    public Submission getLatestSubmission() {
        return submissions.isEmpty() ? null : submissions.get(submissions.size() - 1);
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

    /** Dos usuarios son el mismo si son la misma referencia o tienen el mismo id. */
    private static boolean sameId(User first, User second) {
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
