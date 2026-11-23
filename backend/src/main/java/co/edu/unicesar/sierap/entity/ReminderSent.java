package co.edu.unicesar.sierap.entity;

import java.time.LocalDate;

import co.edu.unicesar.sierap.entity.enums.ReminderKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Constancia de un recordatorio ya encolado (ADR-022).
 *
 * <p>La combinación tipo + referencia + destinatario es única: si el trabajo diario corre
 * dos veces el mismo día, o el servidor se reinicia, el recordatorio no se repite. La
 * referencia es el grupo (cierre de entregas, evaluación pendiente) o el momento (notas
 * publicadas).</p>
 */
@Entity
@Table(name = "reminder_sent", uniqueConstraints = @UniqueConstraint(
        name = "uk_reminder_sent", columnNames = {"kind", "reference_id", "recipient_id"}))
public class ReminderSent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de recordatorio. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderKind kind;

    /** Id del grupo o del momento al que se refiere, según el tipo. */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    /** Id del usuario avisado. */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /** Día en que se encoló. */
    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    /** Constructor requerido por JPA. */
    protected ReminderSent() {
    }

    /**
     * Registra un recordatorio encolado.
     *
     * @param kind        tipo
     * @param referenceId id del grupo o del momento
     * @param recipientId id del usuario avisado
     * @param sentOn      día en que se encoló
     */
    public ReminderSent(ReminderKind kind, Long referenceId, Long recipientId, LocalDate sentOn) {
        this.kind = kind;
        this.referenceId = referenceId;
        this.recipientId = recipientId;
        this.sentOn = sentOn;
    }

    public Long getId() {
        return id;
    }

    public ReminderKind getKind() {
        return kind;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }
}
