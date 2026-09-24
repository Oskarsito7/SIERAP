package co.edu.unicesar.sierap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Institutional key/value parameter defined by the super_admin (RF 11), so general
 * rules are not hard-coded.
 *
 * <p>The columns are {@code param_key} and {@code param_value} because
 * {@code key} and {@code value} are reserved words in SQL.</p>
 */
@Entity
@Table(name = "global_parameter")
public class GlobalParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the parameter, unique in the system. */
    @Column(name = "param_key", nullable = false, unique = true, length = 100)
    private String key;

    /** Value of the parameter, stored as text. */
    @Column(name = "param_value", nullable = false, length = 500)
    private String value;

    /** Explanation of the parameter for whoever manages it. */
    @Column(length = 500)
    private String description;

    /** Constructor required by JPA. */
    protected GlobalParameter() {
    }

    /**
     * Creates a global parameter.
     *
     * @param key         unique name of the parameter
     * @param value       value of the parameter
     * @param description explanation of the parameter
     */
    public GlobalParameter(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    /** @return identifier of the parameter */
    public Long getId() {
        return id;
    }

    /** @return unique name of the parameter */
    public String getKey() {
        return key;
    }

    /** @param key new name of the parameter */
    public void setKey(String key) {
        this.key = key;
    }

    /** @return value of the parameter */
    public String getValue() {
        return value;
    }

    /** @param value new value of the parameter */
    public void setValue(String value) {
        this.value = value;
    }

    /** @return explanation of the parameter */
    public String getDescription() {
        return description;
    }

    /** @param description new explanation of the parameter */
    public void setDescription(String description) {
        this.description = description;
    }
}
