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
 * Nivel de desempeño que un evaluador eligió en un {@link EvaluationCriterion}
 * dentro de una {@link Evaluation}.
 *
 * <p>Guarda una copia del puntaje del nivel en el momento de calificar. Así la nota
 * queda fija aunque después se ajuste la configuración del periodo siguiente
 * (ADR-010), y los reportes no tienen que recorrer los niveles para sumar.</p>
 *
 * <p>Solo se crea desde {@link Evaluation#grade(PerformanceLevel)}, que garantiza
 * una calificación por criterio y que el nivel pertenezca al criterio.</p>
 */
@Entity
@Table(name = "criterion_grade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evaluation_id", "criterion_id"}))
public class CriterionGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evaluación a la que pertenece. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    /** Criterio calificado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    /** Nivel elegido; siempre es uno de los niveles de {@link #criterion}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_level_id", nullable = false)
    private PerformanceLevel level;

    /** Puntaje del nivel al momento de calificar (ADR-013). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** Constructor requerido por JPA. */
    protected CriterionGrade() {
    }

    /**
     * Crea la calificación de un criterio con el nivel elegido.
     *
     * @param level nivel elegido; el criterio calificado es el del nivel
     */
    CriterionGrade(PerformanceLevel level) {
        this.criterion = level.getCriterion();
        changeLevel(level);
    }

    /**
     * Cambia el nivel elegido y copia su puntaje.
     *
     * @param level nuevo nivel; debe pertenecer al mismo criterio
     * @throws IllegalArgumentException si el nivel es de otro criterio (error de
     *                                  programación: el service debe validarlo antes)
     */
    void changeLevel(PerformanceLevel level) {
        EvaluationCriterion levelCriterion = level.getCriterion();
        boolean sameCriterion = levelCriterion == criterion
                || levelCriterion.getId() != null && levelCriterion.getId().equals(criterion.getId());
        if (!sameCriterion) {
            throw new IllegalArgumentException("El nivel no pertenece al criterio calificado");
        }
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
