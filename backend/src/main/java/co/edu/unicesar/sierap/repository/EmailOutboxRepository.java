package co.edu.unicesar.sierap.repository;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la cola de correos (ADR-022).
 */
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    /**
     * Correos en el estado dado cuyo próximo intento ya llegó, los más antiguos primero.
     *
     * @param status estado buscado (normalmente {@code PENDING})
     * @param now    momento actual
     * @param page   tamaño del lote
     * @return correos listos para enviar
     */
    List<EmailOutbox> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
            EmailStatus status, LocalDateTime now, Pageable page);
}
