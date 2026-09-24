package co.edu.unicesar.sierap.entity;

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
import jakarta.persistence.Table;

/**
 * Programa académico (p. ej. Ingeniería de Sistemas), perteneciente a una
 * {@link Faculty}.
 *
 * <p>Es dueño de sus momentos de evaluación (composición): cada {@link Moment}
 * pertenece a un programa en un periodo académico.</p>
 */
@Entity
@Table(name = "program")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código institucional del programa, único en el sistema (p. ej. {@code 108}). */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Nombre del programa. */
    @Column(nullable = false, length = 150)
    private String name;

    /** Facultad a la que pertenece el programa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    /** Momentos de evaluación del programa, de todos los periodos. */
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Moment> moments = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Program() {
    }

    /**
     * Crea un programa sin momentos; se asocia a su facultad con
     * {@link Faculty#addProgram(Program)}.
     *
     * @param code código institucional del programa
     * @param name nombre del programa
     */
    public Program(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Agrega un momento al programa y fija el programa del momento.
     *
     * @param moment momento a agregar
     */
    public void addMoment(Moment moment) {
        moments.add(moment);
        moment.setProgram(this);
    }

    /**
     * Quita un momento del programa; al guardar, el momento se elimina.
     *
     * @param moment momento a quitar
     */
    public void removeMoment(Moment moment) {
        moments.remove(moment);
        moment.setProgram(null);
    }

    /** @return identificador del programa */
    public Long getId() {
        return id;
    }

    /** @return código institucional del programa */
    public String getCode() {
        return code;
    }

    /** @param code nuevo código institucional */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return nombre del programa */
    public String getName() {
        return name;
    }

    /** @param name nuevo nombre del programa */
    public void setName(String name) {
        this.name = name;
    }

    /** @return facultad a la que pertenece */
    public Faculty getFaculty() {
        return faculty;
    }

    /**
     * Fija la facultad del programa. Usar {@link Faculty#addProgram(Program)} para
     * mantener sincronizados los dos lados de la relación.
     *
     * @param faculty facultad dueña del programa
     */
    void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    /** @return momentos del programa, en solo lectura (usar add/remove) */
    public List<Moment> getMoments() {
        return Collections.unmodifiableList(moments);
    }
}
