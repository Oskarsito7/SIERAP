package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Versión de la entrega de un {@link ProjectGroup}: un archivo comprimido con el
 * proyecto y la presentación.
 *
 * <p>Cada carga es una versión nueva; las anteriores se conservan como historial.
 * La combinación grupo + número de versión es única en la base de datos, así que
 * si dos integrantes suben a la vez, la segunda carga falla en lugar de duplicar
 * la versión. Es inmutable: una versión no se edita, se sube otra.</p>
 *
 * <p>Se aceptan cargas mientras la ventana de entregas del momento esté abierta;
 * las que llegan después de la fecha límite del grupo quedan marcadas como
 * tardías ({@link #isLate()}). Las rutas son relativas a la carpeta base de
 * almacenamiento en el disco del servidor (ADR-015).</p>
 */
@Entity
@Table(name = "submission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_group_id", "version_number"}))
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Grupo dueño de la entrega. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_group_id", nullable = false)
    private ProjectGroup projectGroup;

    /** Número de versión, consecutivo por grupo y empezando en 1. */
    @Column(nullable = false)
    private int versionNumber;

    /** Ruta relativa del archivo comprimido con el proyecto. */
    @Column(nullable = false, length = 500)
    private String zipFilePath;

    /** Ruta relativa del archivo de la presentación. */
    @Column(nullable = false, length = 500)
    private String presentationPath;

    /** Fecha y hora de la carga, tomada del {@code Clock} de la aplicación. */
    @Column(nullable = false)
    private LocalDateTime submittedAt;

    /** {@code true} si se cargó después de la fecha límite del grupo. */
    @Column(nullable = false)
    private boolean late;

    /** Constructor requerido por JPA. */
    protected Submission() {
    }

    /**
     * Crea una versión de la entrega. Se asocia al grupo con
     * {@link ProjectGroup#addSubmission(Submission)}.
     *
     * @param versionNumber    número obtenido con {@link ProjectGroup#nextSubmissionVersion()}
     * @param zipFilePath      ruta relativa del archivo comprimido
     * @param presentationPath ruta relativa de la presentación
     * @param submittedAt      fecha y hora de la carga
     * @param late             {@code true} si llegó después de la fecha límite del
     *                         grupo (lo decide el service con el {@code Clock})
     */
    public Submission(int versionNumber, String zipFilePath, String presentationPath,
                      LocalDateTime submittedAt, boolean late) {
        this.versionNumber = versionNumber;
        this.zipFilePath = zipFilePath;
        this.presentationPath = presentationPath;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    public Long getId() {
        return id;
    }

    public ProjectGroup getProjectGroup() {
        return projectGroup;
    }

    void setProjectGroup(ProjectGroup projectGroup) {
        this.projectGroup = projectGroup;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getZipFilePath() {
        return zipFilePath;
    }

    public String getPresentationPath() {
        return presentationPath;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isLate() {
        return late;
    }
}
