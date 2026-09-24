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

/**
 * Grading item within a {@link LearningOutcome} (e.g. "Identificación del
 * problema" in the UPC rubric).
 *
 * <p>The evaluator picks one of its {@link PerformanceLevel}s; the maximum score of
 * the criterion is that of its highest level.</p>
 */
@Entity
@Table(name = "evaluation_criterion")
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Learning outcome that owns the criterion. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_outcome_id", nullable = false)
    private LearningOutcome learningOutcome;

    /** What the criterion assesses. */
    @Column(nullable = false, length = 500)
    private String description;

    /** Position of the criterion within the learning outcome, to show them in order. */
    @Column(nullable = false)
    private int displayOrder;

    /** Levels the evaluator can pick in the criterion. */
    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<PerformanceLevel> levels = new ArrayList<>();

    /** Constructor required by JPA. */
    protected EvaluationCriterion() {
    }

    /**
     * Creates a criterion without levels; it is linked to its learning outcome with
     * {@link LearningOutcome#addCriterion(EvaluationCriterion)}.
     *
     * @param description  what the criterion assesses
     * @param displayOrder position within the learning outcome
     */
    public EvaluationCriterion(String description, int displayOrder) {
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * Maximum score of the criterion: the highest score among its levels.
     *
     * @return the highest {@link PerformanceLevel#getScore()}; zero if it has no levels
     */
    public BigDecimal getMaxScore() {
        return levels.stream()
                .map(PerformanceLevel::getScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Adds a level to the criterion and sets the criterion of the level.
     *
     * @param level level to add
     */
    public void addLevel(PerformanceLevel level) {
        levels.add(level);
        level.setCriterion(this);
    }

    /**
     * Removes a level from the criterion; on save, the level is deleted.
     *
     * @param level level to remove
     */
    public void removeLevel(PerformanceLevel level) {
        levels.remove(level);
        level.setCriterion(null);
    }

    /** @return identifier of the criterion */
    public Long getId() {
        return id;
    }

    /** @return learning outcome that owns the criterion */
    public LearningOutcome getLearningOutcome() {
        return learningOutcome;
    }

    /**
     * Sets the learning outcome of the criterion. Use
     * {@link LearningOutcome#addCriterion(EvaluationCriterion)} to keep both sides
     * of the relationship in sync.
     *
     * @param learningOutcome learning outcome that owns the criterion
     */
    void setLearningOutcome(LearningOutcome learningOutcome) {
        this.learningOutcome = learningOutcome;
    }

    /** @return what the criterion assesses */
    public String getDescription() {
        return description;
    }

    /** @param description new description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return position within the learning outcome */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /** @param displayOrder new position */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /** @return levels of the criterion, read-only (use add/remove) */
    public List<PerformanceLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }
}
