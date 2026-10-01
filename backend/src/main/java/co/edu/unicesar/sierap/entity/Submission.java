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
 * proyecto y, opcionalmente, la presentación.
 *
 * <p>Cada carga es una versión nueva; las anteriores se conservan como historial.
 * La combinación grupo + número de versión es única en la base de datos, así que
 * si dos integrantes suben a la vez, la segunda carga falla en lugar de duplicar
 * la versión. Es inmutable: una versión no se edita, se sube otra.</p>
 *
 * <p>Se aceptan cargas mientras la ventana de entregas del momento esté abierta;
 * las que llegan después de la fecha límite del grupo quedan marcadas como
 * tardías ({@link #isLate()}). Las rutas son relativas a la carpeta base de
 * almacenamiento en el disco del servidor y el nombre en disco lo genera el backend;
 * el nombre original de cada archivo se guarda solo para mostrarlo y para la
 * descarga (ADR-015).</p>
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

    /** Nombre original del archivo comprimido. */
    @Column(nullable = false)
    private String zipFileName;

    /** Tamaño del archivo comprimido, en bytes. */
    @Column(nullable = false)
    private long zipSize;

    /** Ruta relativa de la presentación; {@code null} si esta versión no la trae. */
    @Column(length = 500)
    private String presentationPath;

    /** Nombre original de la presentación; {@code null} si esta versión no la trae. */
    private String presentationFileName;

    /** Tamaño de la presentación, en bytes; {@code null} si esta versión no la trae. */
    private Long presentationSize;

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
     * Crea una versión de la entrega sin presentación. Se asocia al grupo con
     * {@link ProjectGroup#addSubmission(Submission)}.
     *
     * @param versionNumber número obtenido con {@link ProjectGroup#nextSubmissionVersion()}
     * @param zip           archivo comprimido ya guardado en disco
     * @param submittedAt   fecha y hora de la carga
     * @param late          {@code true} si llegó después de la fecha límite del
     *                      grupo (lo decide el service con el {@code Clock})
     */
    public Submission(int versionNumber, StoredFile zip, LocalDateTime submittedAt, boolean late) {
        this.versionNumber = versionNumber;
        this.zipFilePath = zip.path();
        this.zipFileName = zip.originalName();
        this.zipSize = zip.size();
        this.submittedAt = submittedAt;
        this.late = late;
    }

    /**
     * Adjunta la presentación a esta versión, antes de guardarla.
     *
     * @param presentation presentación ya guardada en disco
     */
    public void attachPresentation(StoredFile presentation) {
        this.presentationPath = presentation.path();
        this.presentationFileName = presentation.originalName();
        this.presentationSize = presentation.size();
    }

    /**
     * Indica si esta versión trae presentación.
     *
     * @return {@code true} si se subió una presentación con ella
     */
    public boolean hasPresentation() {
        return presentationPath != null;
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

    public String getZipFileName() {
        return zipFileName;
    }

    public long getZipSize() {
        return zipSize;
    }

    public String getPresentationPath() {
        return presentationPath;
    }

    public String getPresentationFileName() {
        return presentationFileName;
    }

    public Long getPresentationSize() {
        return presentationSize;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isLate() {
        return late;
    }

    /**
     * Archivo de una entrega ya guardado en disco.
     *
     * @param path         ruta relativa a la carpeta base de almacenamiento
     * @param originalName nombre con el que lo subió el estudiante
     * @param size         tamaño en bytes
     */
    public record StoredFile(String path, String originalName, long size) {
    }
}
