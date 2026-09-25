package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Docente: habilita grupos en un momento, designa a sus evaluadores externos, fija
 * las fechas de sus grupos y califica solo los grupos que él habilitó.
 *
 * <p>Pertenece a una sola facultad: en SIERAP solo están los docentes con proyecto
 * de aula, y esos dictan en su facultad.</p>
 */
@Entity
@Table(name = "teacher")
@DiscriminatorValue("TEACHER")
public class Teacher extends User {

    /** Facultad a la que pertenece. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    /** Constructor requerido por JPA. */
    protected Teacher() {
    }

    /**
     * Crea un docente activo de una facultad.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     * @param faculty        facultad a la que pertenece
     */
    public Teacher(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                   String email, String passwordHash, LocalDateTime createdAt,
                   Faculty faculty) {
        super(firstNames, lastNames, documentType, documentNumber, email, passwordHash, createdAt);
        this.faculty = faculty;
    }

    @Override
    public UserRole getRole() {
        return UserRole.TEACHER;
    }

    /** @return facultad a la que pertenece */
    public Faculty getFaculty() {
        return faculty;
    }

    /** @param faculty nueva facultad */
    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }
}
