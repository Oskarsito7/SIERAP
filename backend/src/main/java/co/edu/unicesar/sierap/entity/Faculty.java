package co.edu.unicesar.sierap.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Facultad: unidad académica mayor de la universidad; agrupa uno o varios
 * {@link Program}.
 *
 * <p>Es dueña de sus programas (composición): al eliminar la facultad se eliminan
 * sus programas, y quitar un programa de la lista lo elimina de la base de datos.</p>
 */
@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código institucional de la facultad, único en el sistema (p. ej. {@code 04}). */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Nombre oficial de la facultad. */
    @Column(nullable = false, length = 150)
    private String name;

    /** Programas académicos de la facultad. */
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name")
    private List<Program> programs = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Faculty() {
    }

    /**
     * Crea una facultad sin programas.
     *
     * @param code código institucional de la facultad
     * @param name nombre oficial de la facultad
     */
    public Faculty(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Agrega un programa a la facultad y fija la facultad del programa.
     *
     * @param program programa a agregar
     */
    public void addProgram(Program program) {
        programs.add(program);
        program.setFaculty(this);
    }

    /**
     * Quita un programa de la facultad; al guardar, el programa se elimina.
     *
     * @param program programa a quitar
     */
    public void removeProgram(Program program) {
        programs.remove(program);
        program.setFaculty(null);
    }

    /** @return identificador de la facultad */
    public Long getId() {
        return id;
    }

    /** @return código institucional de la facultad */
    public String getCode() {
        return code;
    }

    /** @param code nuevo código institucional */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return nombre oficial de la facultad */
    public String getName() {
        return name;
    }

    /** @param name nuevo nombre de la facultad */
    public void setName(String name) {
        this.name = name;
    }

    /** @return programas de la facultad, en solo lectura (usar add/remove) */
    public List<Program> getPrograms() {
        return Collections.unmodifiableList(programs);
    }
}
