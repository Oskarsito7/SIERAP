package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Super administrador: administra la institución (parámetros globales, periodos
 * académicos, facultades, programas y el catálogo de tipos de evaluador).
 *
 * <p>No tiene atributos propios; su tabla {@code super_admin} solo guarda la llave
 * que la une con {@code app_user} (herencia {@code JOINED}, ADR-009).</p>
 */
@Entity
@Table(name = "super_admin")
@DiscriminatorValue("SUPER_ADMIN")
public class SuperAdmin extends User {

    /** Constructor requerido por JPA. */
    protected SuperAdmin() {
    }

    /**
     * Crea un super administrador activo.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     */
    public SuperAdmin(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                      String email, String passwordHash, LocalDateTime createdAt) {
        super(firstNames, lastNames, documentType, documentNumber, email, passwordHash, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.SUPER_ADMIN;
    }
}
