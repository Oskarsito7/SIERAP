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
 * Criterio de evaluación: ítem de calificación dentro de un RAP
 * ({@link LearningOutcome}), p. ej. "Identificación del problema" en la rúbrica de
 * la UPC.
 *
 * <p>El evaluador elige uno de sus {@link PerformanceLevel}; el puntaje máximo del
 * criterio es el del nivel más alto.</p>
 */
@Entity
@Table(name = "evaluation_criterion")
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RAP dueño del criterio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_outcome_id", nullable = false)
    private LearningOutcome learningOutcome;

    /** Qué se evalúa en el criterio. */
    @Column(nullable = false, length = 500)
    private String description;

    /** Posición del criterio dentro del RAP, para mostrarlos en orden. */
    @Column(nullable = false)
    private int displayOrder;

    /** Niveles que el evaluador puede elegir en el criterio. */
    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<PerformanceLevel> levels = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected EvaluationCriterion() {
    }

    /**
     * Crea un criterio sin niveles; se asocia a su RAP con
     * {@link LearningOutcome#addCriterion(EvaluationCriterion)}.
     *
     * @param description  qué se evalúa en el criterio
     * @param displayOrder posición del criterio dentro del RAP
     */
    public EvaluationCriterion(String description, int displayOrder) {
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * Puntaje máximo del criterio: el mayor puntaje entre sus niveles.
     *
     * @return el mayor {@link PerformanceLevel#getScore()}; cero si no tiene niveles
     */
    public BigDecimal getMaxScore() {
        return levels.stream()
                .map(PerformanceLevel::getScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Agrega un nivel al criterio y fija el criterio del nivel.
     *
     * @param level nivel a agregar
     */
    public void addLevel(PerformanceLevel level) {
        levels.add(level);
        level.setCriterion(this);
    }

    /**
     * Quita un nivel del criterio; al guardar, el nivel se elimina.
     *
     * @param level nivel a quitar
     */
    public void removeLevel(PerformanceLevel level) {
        levels.remove(level);
        level.setCriterion(null);
    }

    /** @return identificador del criterio */
    public Long getId() {
        return id;
    }

    /** @return RAP dueño del criterio */
    public LearningOutcome getLearningOutcome() {
        return learningOutcome;
    }

    /**
     * Fija el RAP del criterio. Usar
     * {@link LearningOutcome#addCriterion(EvaluationCriterion)} para mantener
     * sincronizados los dos lados de la relación.
     *
     * @param learningOutcome RAP dueño del criterio
     */
    void setLearningOutcome(LearningOutcome learningOutcome) {
        this.learningOutcome = learningOutcome;
    }

    /** @return qué se evalúa en el criterio */
    public String getDescription() {
        return description;
    }

    /** @param description nueva descripción del criterio */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return posición del criterio dentro del RAP */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /** @param displayOrder nueva posición del criterio */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /** @return niveles del criterio, en solo lectura (usar add/remove) */
    public List<PerformanceLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }
}
