package co.edu.unicesar.sierap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tipo de evaluador externo: catálogo que mantiene el super_admin (ADR-011).
 *
 * <p>Los datos semilla son "Empresario" y "Egresado"; agregar un tipo nuevo es
 * agregar una fila, sin programar. Un tipo que ya no se usa se desactiva en lugar
 * de borrarse, porque los evaluadores existentes lo siguen referenciando.</p>
 */
@Entity
@Table(name = "evaluator_type")
public class EvaluatorType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible del tipo, único (p. ej. "Empresario"). */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Descripción opcional del tipo de evaluador. */
    @Column(length = 500)
    private String description;

    /** Indica si el tipo se puede asignar a evaluadores nuevos. */
    @Column(nullable = false)
    private boolean active = true;

    /** Constructor requerido por JPA. */
    protected EvaluatorType() {
    }

    /**
     * Crea un tipo de evaluador activo.
     *
     * @param name        nombre visible del tipo
     * @param description descripción opcional
     */
    public EvaluatorType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** @return identificador del tipo */
    public Long getId() {
        return id;
    }

    /** @return nombre visible del tipo */
    public String getName() {
        return name;
    }

    /** @param name nuevo nombre visible del tipo */
    public void setName(String name) {
        this.name = name;
    }

    /** @return descripción del tipo, o {@code null} */
    public String getDescription() {
        return description;
    }

    /** @param description nueva descripción del tipo */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return {@code true} si el tipo se puede asignar a evaluadores nuevos */
    public boolean isActive() {
        return active;
    }

    /** @param active {@code false} para dejar de ofrecer este tipo */
    public void setActive(boolean active) {
        this.active = active;
    }
}
