package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Usuario de SIERAP: clase base abstracta de los cinco roles del sistema.
 *
 * <p>Se persiste con herencia {@code JOINED} (ADR-009): la tabla {@code app_user}
 * guarda los datos comunes y cada rol tiene su propia tabla con la misma llave
 * primaria. La columna {@code role} es el discriminador; Hibernate la llena con el
 * {@code @DiscriminatorValue} de cada subclase, que coincide con lo que devuelve
 * {@link #getRole()}.</p>
 *
 * <p>No existe un "usuario genérico": solo se instancian las subclases. Todos los
 * usuarios inician sesión con su correo, que se guarda normalizado (sin espacios y
 * en minúsculas) para que la unicidad no dependa de mayúsculas.</p>
 */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombres de la persona. */
    @Column(name = "first_names", nullable = false, length = 100)
    private String firstNames;

    /** Apellidos de la persona. */
    @Column(name = "last_names", nullable = false, length = 100)
    private String lastNames;

    /** Tipo de documento de identidad. */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    /** Número de documento de identidad, único en el sistema. */
    @Column(name = "document_number", nullable = false, unique = true, length = 30)
    private String documentNumber;

    /** Correo con el que el usuario inicia sesión, único y normalizado. */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Hash BCrypt de la contraseña (nunca la contraseña en texto plano). Es
     * {@code null} mientras el usuario no haya usado el enlace para crearla (ADR-017).
     */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /**
     * Indica que la contraseña es temporal y debe cambiarse antes de usar el sistema
     * (el primer super_admin, ADR-017).
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /**
     * Usuario que creó esta cuenta, o el docente dueño del código con el que se
     * registró el estudiante. Es {@code null} para el primer super_admin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** Indica si la cuenta puede iniciar sesión. */
    @Column(nullable = false)
    private boolean active = true;

    /** Fecha y hora de creación de la cuenta, tomada del {@code Clock} del service. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected User() {
    }

    /**
     * Crea un usuario activo con sus datos obligatorios.
     *
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento de identidad
     * @param documentNumber número de documento de identidad
     * @param email          correo de inicio de sesión
     * @param passwordHash   hash de la contraseña, o {@code null} si el usuario la
     *                       creará con el enlace enviado por correo
     * @param createdAt      fecha y hora de creación, obtenida del {@code Clock} inyectado
     */
    protected User(String firstNames, String lastNames, DocumentType documentType, String documentNumber,
                   String email, String passwordHash, LocalDateTime createdAt) {
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        setEmail(email);
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /**
     * Indica el rol del usuario. Cada subclase lo sobrescribe (polimorfismo) y
     * Spring Security decide los permisos con este valor.
     *
     * @return rol del usuario
     */
    public abstract UserRole getRole();

    /**
     * Arma el nombre completo para mostrar en pantalla y en los reportes.
     *
     * @return nombres y apellidos separados por un espacio
     */
    public String getFullName() {
        return firstNames + " " + lastNames;
    }

    /** @return identificador del usuario */
    public Long getId() {
        return id;
    }

    /** @return nombres de la persona */
    public String getFirstNames() {
        return firstNames;
    }

    /** @param firstNames nuevos nombres */
    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    /** @return apellidos de la persona */
    public String getLastNames() {
        return lastNames;
    }

    /** @param lastNames nuevos apellidos */
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    /** @return tipo de documento de identidad */
    public DocumentType getDocumentType() {
        return documentType;
    }

    /** @param documentType nuevo tipo de documento */
    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    /** @return número de documento de identidad */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /** @param documentNumber nuevo número de documento */
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    /** @return correo de inicio de sesión, en minúsculas */
    public String getEmail() {
        return email;
    }

    /**
     * Cambia el correo de inicio de sesión, guardándolo sin espacios y en minúsculas.
     *
     * @param email nuevo correo
     */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** @return hash de la contraseña, o {@code null} si aún no la ha creado */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @param passwordHash nuevo hash de la contraseña */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Indica si el usuario ya creó su contraseña (las cuentas nuevas no pueden iniciar
     * sesión hasta usar el enlace enviado por correo).
     *
     * @return {@code true} si tiene contraseña
     */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /**
     * Reemplaza la contraseña por una definitiva elegida por el usuario: guarda el
     * nuevo hash y quita la marca de contraseña temporal.
     *
     * @param newPasswordHash hash BCrypt de la nueva contraseña
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    /** @return {@code true} si la contraseña es temporal y debe cambiarse */
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /** @param mustChangePassword {@code true} para exigir el cambio de contraseña */
    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    /** @return usuario que creó la cuenta, o {@code null} */
    public User getCreatedBy() {
        return createdBy;
    }

    /** @param createdBy usuario que crea la cuenta */
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    /** @return {@code true} si la cuenta puede iniciar sesión */
    public boolean isActive() {
        return active;
    }

    /** @param active {@code false} para bloquear el inicio de sesión */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return fecha y hora de creación de la cuenta */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
