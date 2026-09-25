package co.edu.unicesar.sierap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Parámetro global: valor institucional clave/valor que define el super_admin
 * (RF 11), para no dejar reglas generales fijas en el código.
 *
 * <p>Las columnas se llaman {@code param_key} y {@code param_value} porque
 * {@code key} y {@code value} son palabras reservadas en SQL.</p>
 */
@Entity
@Table(name = "global_parameter")
public class GlobalParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del parámetro, único en el sistema. */
    @Column(name = "param_key", nullable = false, unique = true, length = 100)
    private String key;

    /** Valor del parámetro, guardado como texto. */
    @Column(name = "param_value", nullable = false, length = 500)
    private String value;

    /** Explicación del parámetro para quien lo administra. */
    @Column(length = 500)
    private String description;

    /** Constructor requerido por JPA. */
    protected GlobalParameter() {
    }

    /**
     * Crea un parámetro global.
     *
     * @param key         nombre único del parámetro
     * @param value       valor del parámetro
     * @param description explicación del parámetro
     */
    public GlobalParameter(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    /** @return identificador del parámetro */
    public Long getId() {
        return id;
    }

    /** @return nombre único del parámetro */
    public String getKey() {
        return key;
    }

    /** @param key nuevo nombre del parámetro */
    public void setKey(String key) {
        this.key = key;
    }

    /** @return valor del parámetro */
    public String getValue() {
        return value;
    }

    /** @param value nuevo valor del parámetro */
    public void setValue(String value) {
        this.value = value;
    }

    /** @return explicación del parámetro */
    public String getDescription() {
        return description;
    }

    /** @param description nueva explicación del parámetro */
    public void setDescription(String description) {
        this.description = description;
    }
}
