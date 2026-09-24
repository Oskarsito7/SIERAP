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
 * Top-level academic unit of the university (facultad); groups one or more
 * {@link Program}s.
 *
 * <p>It owns its programs (composition): deleting the faculty deletes its programs,
 * and removing a program from the list deletes it from the database.</p>
 */
@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Official name of the faculty. */
    @Column(nullable = false, length = 150)
    private String name;

    /** Academic programs of the faculty. */
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name")
    private List<Program> programs = new ArrayList<>();

    /** Constructor required by JPA. */
    protected Faculty() {
    }

    /**
     * Creates a faculty without programs.
     *
     * @param name official name of the faculty
     */
    public Faculty(String name) {
        this.name = name;
    }

    /**
     * Adds a program to the faculty and sets the faculty of the program.
     *
     * @param program program to add
     */
    public void addProgram(Program program) {
        programs.add(program);
        program.setFaculty(this);
    }

    /**
     * Removes a program from the faculty; on save, the program is deleted.
     *
     * @param program program to remove
     */
    public void removeProgram(Program program) {
        programs.remove(program);
        program.setFaculty(null);
    }

    /** @return identifier of the faculty */
    public Long getId() {
        return id;
    }

    /** @return official name of the faculty */
    public String getName() {
        return name;
    }

    /** @param name new name of the faculty */
    public void setName(String name) {
        this.name = name;
    }

    /** @return programs of the faculty, read-only (use add/remove) */
    public List<Program> getPrograms() {
        return Collections.unmodifiableList(programs);
    }
}
