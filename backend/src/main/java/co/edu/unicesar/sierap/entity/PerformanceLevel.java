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

/**
 * Level the evaluator can pick in an {@link EvaluationCriterion} (e.g. Bajo 0 ·
 * Básico 0.36 · Alto 0.48 · Sobresaliente 0.6).
 *
 * <p>Names, number of levels and scores are defined by the admin for each
 * criterion; there are no standard levels in the code (ADR-010). The score is a
 * {@link BigDecimal} so grade sums are exact (ADR-013).</p>
 */
@Entity
@Table(name = "performance_level")
public class PerformanceLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Criterion that owns the level. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    /** Name of the level, e.g. "Sobresaliente". */
    @Column(nullable = false, length = 50)
    private String name;

    /** What the work must meet to get this level. */
    @Column(length = 1000)
    private String description;

    /** Points the level awards. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** Position of the level within the criterion, from lowest to highest. */
    @Column(nullable = false)
    private int displayOrder;

    /** Constructor required by JPA. */
    protected PerformanceLevel() {
    }

    /**
     * Creates a performance level; it is linked to its criterion with
     * {@link EvaluationCriterion#addLevel(PerformanceLevel)}.
     *
     * @param name         name of the level
     * @param description  what the work must meet to get it
     * @param score        points it awards
     * @param displayOrder position within the criterion
     */
    public PerformanceLevel(String name, String description, BigDecimal score, int displayOrder) {
        this.name = name;
        this.description = description;
        this.score = score;
        this.displayOrder = displayOrder;
    }

    /** @return identifier of the level */
    public Long getId() {
        return id;
    }

    /** @return criterion that owns the level */
    public EvaluationCriterion getCriterion() {
        return criterion;
    }

    /**
     * Sets the criterion of the level. Use
     * {@link EvaluationCriterion#addLevel(PerformanceLevel)} to keep both sides of
     * the relationship in sync.
     *
     * @param criterion criterion that owns the level
     */
    void setCriterion(EvaluationCriterion criterion) {
        this.criterion = criterion;
    }

    /** @return name of the level */
    public String getName() {
        return name;
    }

    /** @param name new name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return what the work must meet to get the level */
    public String getDescription() {
        return description;
    }

    /** @param description new description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return points the level awards */
    public BigDecimal getScore() {
        return score;
    }

    /** @param score new points */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    /** @return position within the criterion */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /** @param displayOrder new position */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
