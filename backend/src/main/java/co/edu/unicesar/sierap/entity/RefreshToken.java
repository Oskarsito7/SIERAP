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

/**
 * Token de renovación de sesión (ADR-017): permite obtener un nuevo token de acceso
 * sin volver a escribir la contraseña.
 *
 * <p>Solo se guarda el hash SHA-256 del valor que viaja en la cookie, así que un
 * volcado de la base de datos no permite suplantar sesiones.</p>
 *
 * <p>Cada renovación lo marca como rotado y crea uno nuevo. El rotado se conserva
 * hasta que vence para detectar si alguien lo vuelve a usar (señal de robo). Cerrar
 * sesión, cambiar la contraseña o desactivar la cuenta, en cambio, borran la fila:
 * un token cerrado a propósito solo deja de servir, sin disparar esa alarma.</p>
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario dueño de la sesión. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash SHA-256 (hexadecimal) del valor del token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Momento a partir del cual deja de servir. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Momento en que se cambió por uno nuevo, o {@code null} si sigue vigente. */
    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    /** Fecha y hora de creación. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected RefreshToken() {
    }

    /**
     * Crea un token vigente.
     *
     * @param user      usuario dueño de la sesión
     * @param tokenHash hash SHA-256 del valor del token
     * @param expiresAt momento de vencimiento
     * @param createdAt momento de creación
     */
    public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /**
     * Indica si el token todavía sirve para renovar la sesión.
     *
     * @param now momento actual, obtenido del {@code Clock} inyectado
     * @return {@code true} si no fue rotado ni está vencido
     */
    public boolean isActiveAt(LocalDateTime now) {
        return rotatedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Indica si el token ya se cambió por uno nuevo.
     *
     * @return {@code true} si fue rotado
     */
    public boolean isRotated() {
        return rotatedAt != null;
    }

    /**
     * Marca el token como rotado; si ya lo estaba, conserva la primera fecha.
     *
     * @param now momento actual
     */
    public void markRotated(LocalDateTime now) {
        if (rotatedAt == null) {
            rotatedAt = now;
        }
    }

    /** @return identificador del token */
    public Long getId() {
        return id;
    }

    /** @return usuario dueño de la sesión */
    public User getUser() {
        return user;
    }

    /** @return hash del valor del token */
    public String getTokenHash() {
        return tokenHash;
    }

    /** @return momento de vencimiento */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /** @return momento de rotación, o {@code null} */
    public LocalDateTime getRotatedAt() {
        return rotatedAt;
    }
}
