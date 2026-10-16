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
 * Peso de un {@link EvaluatorType} en la nota final de un {@link Moment} (ADR-018).
 *
 * <p>Es un porcentaje mayor que cero y hasta 100, con dos decimales. El peso del
 * docente lo guarda el propio momento; junto con los pesos de los tipos debe sumar
 * 100 para poder lanzar el momento. Un tipo sin peso en el momento no participa en
 * él. Se crea y se modifica solo a través de
 * {@link Moment#setEvaluatorWeight(EvaluatorType, BigDecimal)}.</p>
 */
@Entity
@Table(name = "evaluator_weight",
        uniqueConstraints = @UniqueConstraint(columnNames = {"moment_id", "evaluator_type_id"}))
public class EvaluatorWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Momento dueño del peso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id", nullable = false)
    private Moment moment;

    /** Tipo de evaluador al que se le asigna el peso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_type_id", nullable = false)
    private EvaluatorType evaluatorType;

    /** Porcentaje de la nota final que aporta el tipo de evaluador. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    /** Constructor requerido por JPA. */
    protected EvaluatorWeight() {
    }

    /**
     * Crea el peso de un tipo de evaluador en un momento.
     *
     * @param moment        momento dueño del peso
     * @param evaluatorType tipo de evaluador
     * @param weight        porcentaje, ya validado por el momento
     */
    EvaluatorWeight(Moment moment, EvaluatorType evaluatorType, BigDecimal weight) {
        this.moment = moment;
        this.evaluatorType = evaluatorType;
        this.weight = weight;
    }

    /** @return identificador del peso */
    public Long getId() {
        return id;
    }

    /** @return momento dueño del peso */
    public Moment getMoment() {
        return moment;
    }

    /** @return tipo de evaluador al que se le asigna el peso */
    public EvaluatorType getEvaluatorType() {
        return evaluatorType;
    }

    /** @return porcentaje de la nota final que aporta el tipo de evaluador */
    public BigDecimal getWeight() {
        return weight;
    }

    /**
     * Cambia el porcentaje; lo usa el momento, que valida el valor y su estado.
     *
     * @param weight nuevo porcentaje
     */
    void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
