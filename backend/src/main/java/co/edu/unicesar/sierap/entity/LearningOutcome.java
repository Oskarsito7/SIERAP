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
 * Program learning outcome (RAP, Resultado de Aprendizaje del Programa) assessed in
 * a {@link Moment}.
 *
 * <p>Each moment has its own learning outcomes, which the admin creates, edits or
 * deletes while the moment is in configuration. It has no fixed weight: it weighs
 * the sum of the maximum scores of its {@link EvaluationCriterion}s (ADR-010).</p>
 */
@Entity
@Table(name = "learning_outcome",
        uniqueConstraints = @UniqueConstraint(columnNames = {"moment_id", "code"}))
public class LearningOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Moment that owns the learning outcome. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id", nullable = false)
    private Moment moment;

    /** Code of the learning outcome within the moment, e.g. {@code RAP1}. */
    @Column(nullable = false, length = 20)
    private String code;

    /** Statement of the learning outcome. */
    @Column(nullable = false, length = 1000)
    private String description;

    /** Criteria used to grade the learning outcome. */
    @OneToMany(mappedBy = "learningOutcome", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<EvaluationCriterion> criteria = new ArrayList<>();

    /** Constructor required by JPA. */
    protected LearningOutcome() {
    }

    /**
     * Creates a learning outcome without criteria; it is linked to its moment with
     * {@link Moment#addLearningOutcome(LearningOutcome)}.
     *
     * @param code        code within the moment
     * @param description statement of the learning outcome
     */
    public LearningOutcome(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Maximum score that can be obtained in the learning outcome: the sum of the
     * maximum scores of its criteria.
     *
     * @return sum of {@link EvaluationCriterion#getMaxScore()}; zero if it has no
     *         criteria
     */
    public BigDecimal getMaxScore() {
        return criteria.stream()
                .map(EvaluationCriterion::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adds a criterion to the learning outcome and sets its learning outcome.
     *
     * @param criterion criterion to add
     */
    public void addCriterion(EvaluationCriterion criterion) {
        criteria.add(criterion);
        criterion.setLearningOutcome(this);
    }

    /**
     * Removes a criterion from the learning outcome; on save, it is deleted
     * together with its levels.
     *
     * @param criterion criterion to remove
     */
    public void removeCriterion(EvaluationCriterion criterion) {
        criteria.remove(criterion);
        criterion.setLearningOutcome(null);
    }

    /** @return identifier of the learning outcome */
    public Long getId() {
        return id;
    }

    /** @return moment that owns the learning outcome */
    public Moment getMoment() {
        return moment;
    }

    /**
     * Sets the moment of the learning outcome. Use
     * {@link Moment#addLearningOutcome(LearningOutcome)} to keep both sides of the
     * relationship in sync.
     *
     * @param moment moment that owns the learning outcome
     */
    void setMoment(Moment moment) {
        this.moment = moment;
    }

    /** @return code of the learning outcome */
    public String getCode() {
        return code;
    }

    /** @param code new code */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return statement of the learning outcome */
    public String getDescription() {
        return description;
    }

    /** @param description new statement */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return criteria of the learning outcome, read-only (use add/remove) */
    public List<EvaluationCriterion> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }
}
