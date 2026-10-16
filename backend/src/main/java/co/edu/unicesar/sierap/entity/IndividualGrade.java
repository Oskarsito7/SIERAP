package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Ajuste individual (ADR-018): el nivel que un evaluador le da a <b>un estudiante</b>
 * en un criterio cuando su desempeño fue distinto al del grupo.
 *
 * <p>Por defecto cada integrante hereda los niveles del grupo
 * ({@link CriterionGrade}); un ajuste reemplaza el nivel del grupo solo para ese
 * estudiante y ese criterio. Como {@link CriterionGrade}, guarda una copia del
 * puntaje del nivel para que la nota no cambie si después se ajusta la configuración
 * (ADR-010).</p>
 *
 * <p>Solo se crea desde {@link Evaluation#adjust(Student, PerformanceLevel)}, que
 * garantiza un ajuste por estudiante y criterio, que el estudiante sea integrante del
 * grupo y que el nivel sea distinto al del grupo.</p>
 */
@Entity
@Table(name = "individual_grade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evaluation_id", "student_id", "criterion_id"}))
public class IndividualGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evaluación a la que pertenece el ajuste. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    /** Integrante del grupo al que se le ajusta el nivel. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Criterio ajustado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    /** Nivel para este estudiante; siempre es uno de los niveles de {@link #criterion}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_level_id", nullable = false)
    private PerformanceLevel level;

    /** Puntaje del nivel al momento de ajustar (ADR-013). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** Constructor requerido por JPA. */
    protected IndividualGrade() {
    }

    /**
     * Crea el ajuste de un estudiante con el nivel elegido.
     *
     * @param student estudiante ajustado
     * @param level   nivel elegido; el criterio ajustado es el del nivel
     */
    IndividualGrade(Student student, PerformanceLevel level) {
        this.student = student;
        this.criterion = level.getCriterion();
        this.level = level;
        this.score = level.getScore();
    }

    /**
     * Cambia el nivel del ajuste y copia su puntaje. El nivel es del mismo criterio:
     * {@link Evaluation#adjust(Student, PerformanceLevel)} busca el ajuste por criterio.
     *
     * @param level nuevo nivel
     */
    void changeLevel(PerformanceLevel level) {
        this.level = level;
        this.score = level.getScore();
    }

    public Long getId() {
        return id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public Student getStudent() {
        return student;
    }

    public EvaluationCriterion getCriterion() {
        return criterion;
    }

    public PerformanceLevel getLevel() {
        return level;
    }

    public BigDecimal getScore() {
        return score;
    }
}
