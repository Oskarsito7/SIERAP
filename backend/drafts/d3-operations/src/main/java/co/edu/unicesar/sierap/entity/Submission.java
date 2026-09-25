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
 * <p>Las rutas apuntan al almacenamiento de archivos; dónde se guardan (disco,
 * almacenamiento de objetos…) está pendiente de un ADR.</p>
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

    /** Ruta del archivo comprimido con el proyecto. */
    @Column(nullable = false, length = 500)
    private String zipFilePath;

    /** Ruta del archivo de la presentación. */
    @Column(nullable = false, length = 500)
    private String presentationPath;

    /** Fecha y hora de la carga, tomada del {@code Clock} de la aplicación. */
    @Column(nullable = false)
    private LocalDateTime submittedAt;

    /** Constructor requerido por JPA. */
    protected Submission() {
    }

    /**
     * Crea una versión de la entrega. Se asocia al grupo con
     * {@link ProjectGroup#addSubmission(Submission)}.
     *
     * @param versionNumber    número obtenido con {@link ProjectGroup#nextSubmissionVersion()}
     * @param zipFilePath      ruta del archivo comprimido
     * @param presentationPath ruta de la presentación
     * @param submittedAt      fecha y hora de la carga
     */
    public Submission(int versionNumber, String zipFilePath, String presentationPath,
                      LocalDateTime submittedAt) {
        this.versionNumber = versionNumber;
        this.zipFilePath = zipFilePath;
        this.presentationPath = presentationPath;
        this.submittedAt = submittedAt;
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
}
