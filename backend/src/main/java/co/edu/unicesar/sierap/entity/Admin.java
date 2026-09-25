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
 * Administrador de una facultad (rol del decano): crea, configura y lanza los
 * momentos de los programas de su facultad.
 *
 * <p>Una facultad puede tener varios administradores, todos con los mismos permisos.
 * Solo gestiona los programas, momentos y RAP de <b>su</b> facultad.</p>
 */
@Entity
@Table(name = "admin")
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    /** Facultad a la que pertenece. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    /** Constructor requerido por JPA. */
    protected Admin() {
    }

    /**
     * Crea un administrador activo de una facultad.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     * @param faculty        facultad que administra
     */
    public Admin(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                 String email, String passwordHash, LocalDateTime createdAt,
                 Faculty faculty) {
        super(firstNames, lastNames, documentType, documentNumber, email, passwordHash, createdAt);
        this.faculty = faculty;
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
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
