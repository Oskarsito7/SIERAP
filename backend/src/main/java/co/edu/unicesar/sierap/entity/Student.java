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
 * Estudiante: pertenece a un programa, se inscribe en un grupo por momento, sube las
 * entregas de su grupo y consulta su desempeño por RAP.
 *
 * <p>No implementa {@code Evaluator}: en SIERAP el estudiante no se autoevalúa. Su
 * historial de grupos queda del lado de {@code ProjectGroup} (un grupo por momento).</p>
 */
@Entity
@Table(name = "student")
@DiscriminatorValue("STUDENT")
public class Student extends User {

    /** Código estudiantil institucional, único. */
    @Column(name = "student_code", nullable = false, unique = true, length = 30)
    private String studentCode;

    /** Semestre que cursa actualmente (1 o mayor). */
    @Column(name = "current_semester", nullable = false)
    private int currentSemester;

    /** Programa académico que cursa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** Constructor requerido por JPA. */
    protected Student() {
    }

    /**
     * Crea un estudiante activo de un programa.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     * @param studentCode     código estudiantil
     * @param currentSemester semestre que cursa
     * @param program         programa que cursa
     */
    public Student(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                   String email, String passwordHash, LocalDateTime createdAt,
                   String studentCode, int currentSemester, Program program) {
        super(firstNames, lastNames, documentType, documentNumber, email, passwordHash, createdAt);
        this.studentCode = studentCode;
        this.currentSemester = currentSemester;
        this.program = program;
    }

    @Override
    public UserRole getRole() {
        return UserRole.STUDENT;
    }

    /** @return código estudiantil */
    public String getStudentCode() {
        return studentCode;
    }

    /** @param studentCode nuevo código estudiantil */
    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    /** @return semestre que cursa actualmente */
    public int getCurrentSemester() {
        return currentSemester;
    }

    /** @param currentSemester nuevo semestre actual */
    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    /** @return programa que cursa */
    public Program getProgram() {
        return program;
    }

    /** @param program nuevo programa */
    public void setProgram(Program program) {
        this.program = program;
    }
}
