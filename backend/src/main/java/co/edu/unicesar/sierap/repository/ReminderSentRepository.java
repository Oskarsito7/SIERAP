package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.ReminderSent;
import co.edu.unicesar.sierap.entity.enums.ReminderKind;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de los recordatorios ya encolados (ADR-022).
 */
public interface ReminderSentRepository extends JpaRepository<ReminderSent, Long> {

    /**
     * Indica si ese recordatorio ya se encoló para ese destinatario.
     *
     * @param kind        tipo de recordatorio
     * @param referenceId id del grupo o del momento
     * @param recipientId id del usuario
     * @return {@code true} si ya existe la constancia
     */
    boolean existsByKindAndReferenceIdAndRecipientId(ReminderKind kind, Long referenceId, Long recipientId);
}
