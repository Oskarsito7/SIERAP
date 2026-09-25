package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * RAP (Resultado de Aprendizaje del Programa) evaluado en un {@link Moment}.
 *
 * <p>Cada momento tiene sus propios RAP, que el admin crea, edita o elimina
 * mientras el momento está en configuración. El RAP no tiene un peso fijo: pesa lo
 * que sumen los puntajes máximos de sus {@link EvaluationCriterion} (ADR-010).</p>
 */
@Entity
@Table(name = "learning_outcome",
        uniqueConstraints = @UniqueConstraint(columnNames = {"moment_id", "code"}))
public class LearningOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Momento dueño del RAP. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id", nullable = false)
    private Moment moment;

    /** Código del RAP dentro del momento, p. ej. {@code RAP1}. */
    @Column(nullable = false, length = 20)
    private String code;

    /** Enunciado del resultado de aprendizaje. */
    @Column(nullable = false, length = 1000)
    private String description;

    /** Criterios con los que se califica el RAP. */
    @OneToMany(mappedBy = "learningOutcome", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<EvaluationCriterion> criteria = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected LearningOutcome() {
    }

    /**
     * Crea un RAP sin criterios; se asocia a su momento con
     * {@link Moment#addLearningOutcome(LearningOutcome)}.
     *
     * @param code        código del RAP dentro del momento
     * @param description enunciado del resultado de aprendizaje
     */
    public LearningOutcome(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Puntaje máximo que se puede obtener en el RAP: la suma de los puntajes
     * máximos de sus criterios.
     *
     * @return suma de {@link EvaluationCriterion#getMaxScore()}; cero si no tiene
     *         criterios
     */
    public BigDecimal getMaxScore() {
        return criteria.stream()
                .map(EvaluationCriterion::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un criterio al RAP y fija el RAP del criterio.
     *
     * @param criterion criterio a agregar
     */
    public void addCriterion(EvaluationCriterion criterion) {
        criteria.add(criterion);
        criterion.setLearningOutcome(this);
    }

    /**
     * Quita un criterio del RAP; al guardar, el criterio se elimina con sus niveles.
     *
     * @param criterion criterio a quitar
     */
    public void removeCriterion(EvaluationCriterion criterion) {
        criteria.remove(criterion);
        criterion.setLearningOutcome(null);
    }

    /** @return identificador del RAP */
    public Long getId() {
        return id;
    }

    /** @return momento dueño del RAP */
    public Moment getMoment() {
        return moment;
    }

    /**
     * Fija el momento del RAP. Usar {@link Moment#addLearningOutcome(LearningOutcome)}
     * para mantener sincronizados los dos lados de la relación.
     *
     * @param moment momento dueño del RAP
     */
    void setMoment(Moment moment) {
        this.moment = moment;
    }

    /** @return código del RAP */
    public String getCode() {
        return code;
    }

    /** @param code nuevo código del RAP */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return enunciado del resultado de aprendizaje */
    public String getDescription() {
        return description;
    }

    /** @param description nuevo enunciado */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return criterios del RAP, en solo lectura (usar add/remove) */
    public List<EvaluationCriterion> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }
}
