package co.edu.unicesar.sierap.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Correo en la cola de salida (outbox transaccional, ADR-022).
 *
 * <p>Se inserta dentro de la transacción de la operación que lo pide, de modo que si
 * esta se deshace el correo desaparece con ella. Un despachador programado lo envía
 * después; si el servidor SMTP falla, se reintenta con espera exponencial (1, 2, 4,
 * 8 minutos…) hasta agotar los intentos permitidos, y entonces queda en
 * {@link EmailStatus#FAILED}.</p>
 */
@Entity
@Table(name = "email_outbox")
public class EmailOutbox {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo del destinatario. */
    @Column(nullable = false)
    private String recipient;

    /** Asunto. */
    @Column(nullable = false)
    private String subject;

    /** Cuerpo en texto plano. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Estado del envío. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmailStatus status = EmailStatus.PENDING;

    /** Intentos de envío fallidos hasta ahora. */
    @Column(nullable = false)
    private int attempts;

    /** Momento a partir del cual el despachador puede (re)intentar el envío. */
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    /** Mensaje del último error de envío, o {@code null} si no ha fallado. */
    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    /** Momento en que se encoló. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Momento en que se envió, o {@code null} si aún no. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Constructor requerido por JPA. */
    protected EmailOutbox() {
    }

    /**
     * Crea un correo pendiente, listo para enviarse de inmediato.
     *
     * @param recipient correo del destinatario
     * @param subject   asunto
     * @param body      cuerpo en texto plano
     * @param now       momento actual
     */
    public EmailOutbox(String recipient, String subject, String body, LocalDateTime now) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    /**
     * Espera antes del reintento tras el fallo número {@code failedAttempts}: 1, 2, 4,
     * 8… minutos.
     *
     * @param failedAttempts intentos fallidos acumulados (mínimo 1)
     * @return espera hasta el siguiente intento
     */
    public static Duration backoffFor(int failedAttempts) {
        int exponent = Math.min(Math.max(failedAttempts, 1) - 1, 20);
        return Duration.ofMinutes(1L << exponent);
    }

    /**
     * Marca el correo como enviado.
     *
     * @param now momento del envío
     */
    public void markSent(LocalDateTime now) {
        this.status = EmailStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    /**
     * Registra un intento fallido: sube el contador y programa el reintento con espera
     * exponencial, o pasa a {@link EmailStatus#FAILED} si se agotaron los intentos.
     *
     * @param now         momento del fallo
     * @param error       mensaje del error (se recorta a 1000 caracteres)
     * @param maxAttempts intentos permitidos antes de rendirse
     */
    public void markFailedAttempt(LocalDateTime now, String error, int maxAttempts) {
        this.attempts++;
        this.lastError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (attempts >= maxAttempts) {
            this.status = EmailStatus.FAILED;
        } else {
            this.nextAttemptAt = now.plus(backoffFor(attempts));
        }
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
