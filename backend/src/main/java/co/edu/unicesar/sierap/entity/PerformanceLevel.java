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
 * Nivel de desempeño que el evaluador puede elegir en un
 * {@link EvaluationCriterion} (p. ej. Bajo 0 · Básico 0.36 · Alto 0.48 ·
 * Sobresaliente 0.6).
 *
 * <p>Nombres, cantidad y puntajes los define el admin en cada criterio; no hay
 * niveles estándar en el código (ADR-010). El puntaje es {@link BigDecimal} para
 * que las sumas de la nota sean exactas (ADR-013).</p>
 */
@Entity
@Table(name = "performance_level")
public class PerformanceLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Criterio dueño del nivel. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    /** Nombre del nivel, p. ej. "Sobresaliente". */
    @Column(nullable = false, length = 50)
    private String name;

    /** Qué debe cumplir el trabajo para obtener este nivel. */
    @Column(length = 1000)
    private String description;

    /** Puntos que otorga el nivel. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** Posición del nivel dentro del criterio, del más bajo al más alto. */
    @Column(nullable = false)
    private int displayOrder;

    /** Constructor requerido por JPA. */
    protected PerformanceLevel() {
    }

    /**
     * Crea un nivel de desempeño; se asocia a su criterio con
     * {@link EvaluationCriterion#addLevel(PerformanceLevel)}.
     *
     * @param name         nombre del nivel
     * @param description  qué debe cumplir el trabajo para obtenerlo
     * @param score        puntos que otorga
     * @param displayOrder posición dentro del criterio
     */
    public PerformanceLevel(String name, String description, BigDecimal score, int displayOrder) {
        this.name = name;
        this.description = description;
        this.score = score;
        this.displayOrder = displayOrder;
    }

    /** @return identificador del nivel */
    public Long getId() {
        return id;
    }

    /** @return criterio dueño del nivel */
    public EvaluationCriterion getCriterion() {
        return criterion;
    }

    /**
     * Fija el criterio del nivel. Usar
     * {@link EvaluationCriterion#addLevel(PerformanceLevel)} para mantener
     * sincronizados los dos lados de la relación.
     *
     * @param criterion criterio dueño del nivel
     */
    void setCriterion(EvaluationCriterion criterion) {
        this.criterion = criterion;
    }

    /** @return nombre del nivel */
    public String getName() {
        return name;
    }

    /** @param name nuevo nombre del nivel */
    public void setName(String name) {
        this.name = name;
    }

    /** @return qué debe cumplir el trabajo para obtener el nivel */
    public String getDescription() {
        return description;
    }

    /** @param description nueva descripción del nivel */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return puntos que otorga el nivel */
    public BigDecimal getScore() {
        return score;
    }

    /** @param score nuevos puntos del nivel */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    /** @return posición dentro del criterio */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /** @param displayOrder nueva posición dentro del criterio */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
