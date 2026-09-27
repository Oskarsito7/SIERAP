package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token de un solo uso del enlace enviado por correo para crear o recuperar la
 * contraseña (ADR-017).
 *
 * <p>Como en {@link RefreshToken}, solo se guarda el hash SHA-256 del valor que va
 * en el enlace.</p>
 */
@Entity
@Table(name = "password_token")
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario al que pertenece el enlace. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash SHA-256 (hexadecimal) del valor del token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Para qué sirve el enlace. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PasswordTokenPurpose purpose;

    /** Momento a partir del cual el enlace deja de servir. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Momento en que se usó (o se invalidó), o {@code null} si sigue disponible. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Fecha y hora de creación. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected PasswordToken() {
    }

    /**
     * Crea un enlace disponible.
     *
     * @param user      usuario al que pertenece
     * @param tokenHash hash SHA-256 del valor del token
     * @param purpose   para qué sirve
     * @param expiresAt momento de vencimiento
     * @param createdAt momento de creación
     */
    public PasswordToken(User user, String tokenHash, PasswordTokenPurpose purpose, LocalDateTime expiresAt,
                         LocalDateTime createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /**
     * Indica si el enlace todavía se puede usar.
     *
     * @param now momento actual, obtenido del {@code Clock} inyectado
     * @return {@code true} si no se ha usado y no ha vencido
     */
    public boolean isUsableAt(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Marca el enlace como usado (o invalidado por uno más reciente).
     *
     * @param now momento actual
     */
    public void markUsed(LocalDateTime now) {
        if (usedAt == null) {
            usedAt = now;
        }
    }

    /** @return identificador del token */
    public Long getId() {
        return id;
    }

    /** @return usuario al que pertenece */
    public User getUser() {
        return user;
    }

    /** @return para qué sirve el enlace */
    public PasswordTokenPurpose getPurpose() {
        return purpose;
    }

    /** @return momento de vencimiento */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /** @return momento de uso, o {@code null} */
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
