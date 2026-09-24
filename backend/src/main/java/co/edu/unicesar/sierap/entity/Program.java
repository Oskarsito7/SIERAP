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
 * Academic program (e.g. Ingeniería de Sistemas) that belongs to a
 * {@link Faculty}.
 *
 * <p>It owns its assessment moments (composition): each {@link Moment} belongs to
 * a program in an academic period.</p>
 */
@Entity
@Table(name = "program")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Institutional code of the program, unique in the system. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Name of the program. */
    @Column(nullable = false, length = 150)
    private String name;

    /** Faculty the program belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    /** Assessment moments of the program, across all periods. */
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Moment> moments = new ArrayList<>();

    /** Constructor required by JPA. */
    protected Program() {
    }

    /**
     * Creates a program without moments; it is linked to its faculty with
     * {@link Faculty#addProgram(Program)}.
     *
     * @param code institutional code of the program
     * @param name name of the program
     */
    public Program(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Adds a moment to the program and sets the program of the moment.
     *
     * @param moment moment to add
     */
    public void addMoment(Moment moment) {
        moments.add(moment);
        moment.setProgram(this);
    }

    /**
     * Removes a moment from the program; on save, the moment is deleted.
     *
     * @param moment moment to remove
     */
    public void removeMoment(Moment moment) {
        moments.remove(moment);
        moment.setProgram(null);
    }

    /** @return identifier of the program */
    public Long getId() {
        return id;
    }

    /** @return institutional code of the program */
    public String getCode() {
        return code;
    }

    /** @param code new institutional code */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return name of the program */
    public String getName() {
        return name;
    }

    /** @param name new name of the program */
    public void setName(String name) {
        this.name = name;
    }

    /** @return faculty the program belongs to */
    public Faculty getFaculty() {
        return faculty;
    }

    /**
     * Sets the faculty of the program. Use {@link Faculty#addProgram(Program)} to
     * keep both sides of the relationship in sync.
     *
     * @param faculty faculty that owns the program
     */
    void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    /** @return moments of the program, read-only (use add/remove) */
    public List<Moment> getMoments() {
        return Collections.unmodifiableList(moments);
    }
}
