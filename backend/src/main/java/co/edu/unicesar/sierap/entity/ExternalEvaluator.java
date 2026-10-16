package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Evaluador externo: evaluador que no es docente (hoy empresarios y egresados).
 *
 * <p>Lo crea un docente y califica solo los grupos para los que fue designado. Su
 * tipo sale del catálogo {@link EvaluatorType} (ADR-011), así que un tipo nuevo no
 * requiere programar. Los datos propios de cada tipo (organización, cargo, año y
 * programa de egreso) son opcionales.</p>
 */
@Entity
@Table(name = "external_evaluator")
@DiscriminatorValue("EXTERNAL_EVALUATOR")
public class ExternalEvaluator extends User implements Evaluator {

    /** Tipo de evaluador del catálogo (p. ej. "Empresario"). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_type_id", nullable = false)
    private EvaluatorType evaluatorType;

    /** Empresa u organización a la que pertenece (opcional). */
    @Column(length = 150)
    private String organization;

    /** Cargo en su organización (opcional). */
    @Column(name = "job_position", length = 100)
    private String position;

    /** Año de graduación, si es egresado (opcional). */
    @Column(name = "graduation_year")
    private Integer graduationYear;

    /** Programa de la UPC del que egresó, si es egresado (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduation_program_id")
    private Program graduationProgram;

    /** Constructor requerido por JPA. */
    protected ExternalEvaluator() {
    }

    /**
     * Crea un evaluador externo activo de un tipo del catálogo; los datos opcionales se fijan con sus setters.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     * @param evaluatorType  tipo de evaluador del catálogo
     */
    public ExternalEvaluator(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                             String email, String passwordHash, LocalDateTime createdAt,
                             EvaluatorType evaluatorType) {
        super(firstNames, lastNames, documentType, documentNumber, email, passwordHash, createdAt);
        this.evaluatorType = evaluatorType;
    }

    @Override
    public UserRole getRole() {
        return UserRole.EXTERNAL_EVALUATOR;
    }

    /**
     * Un evaluador externo califica solo los grupos para los que fue designado.
     *
     * @param group grupo que se quiere calificar
     * @return {@code true} si este evaluador está entre los designados del grupo
     */
    @Override
    public boolean canEvaluate(ProjectGroup group) {
        return group.hasEvaluator(this);
    }

    /** @return tipo de evaluador del catálogo */
    public EvaluatorType getEvaluatorType() {
        return evaluatorType;
    }

    /** @param evaluatorType nuevo tipo de evaluador */
    public void setEvaluatorType(EvaluatorType evaluatorType) {
        this.evaluatorType = evaluatorType;
    }

    /** @return organización, o {@code null} */
    public String getOrganization() {
        return organization;
    }

    /** @param organization nueva organización */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /** @return cargo, o {@code null} */
    public String getPosition() {
        return position;
    }

    /** @param position nuevo cargo */
    public void setPosition(String position) {
        this.position = position;
    }

    /** @return año de graduación, o {@code null} */
    public Integer getGraduationYear() {
        return graduationYear;
    }

    /** @param graduationYear nuevo año de graduación */
    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    /** @return programa de egreso, o {@code null} */
    public Program getGraduationProgram() {
        return graduationProgram;
    }

    /** @param graduationProgram nuevo programa de egreso */
    public void setGraduationProgram(Program graduationProgram) {
        this.graduationProgram = graduationProgram;
    }
}
