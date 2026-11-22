package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.config.MailOutboxProperties;
import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Despachador programado de la cola de correos (ADR-022).
 *
 * <p>Cada ronda toma hasta {@code batch-size} correos {@code PENDING} cuyo próximo intento
 * ya llegó y los entrega uno a uno con {@link EmailOutboxProcessor}, cada uno en su
 * transacción. Se desactiva con {@code sierap.mail.outbox.enabled=false}.</p>
 */
@Component
@ConditionalOnProperty(prefix = "sierap.mail.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailOutboxDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOutboxDispatcher.class);

    private final EmailOutboxRepository outboxRepository;
    private final EmailOutboxProcessor processor;
    private final MailOutboxProperties properties;
    private final Clock clock;

    /**
     * Crea el despachador.
     *
     * @param outboxRepository repositorio de la cola de correos
     * @param processor        entrega de un correo en su propia transacción
     * @param properties       parámetros de la cola (tamaño del lote)
     * @param clock            reloj de la aplicación
     */
    public EmailOutboxDispatcher(EmailOutboxRepository outboxRepository, EmailOutboxProcessor processor,
                                 MailOutboxProperties properties, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.processor = processor;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Ronda del despachador. La espera entre rondas es {@code sierap.mail.outbox.dispatch-delay}.
     *
     * @return cantidad de correos que se intentaron en esta ronda
     */
    @Scheduled(fixedDelayString = "${sierap.mail.outbox.dispatch-delay:PT15S}")
    public int dispatch() {
        List<EmailOutbox> due = outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                EmailStatus.PENDING, LocalDateTime.now(clock), PageRequest.of(0, properties.batchSize()));
        for (EmailOutbox email : due) {
            try {
                processor.deliver(email.getId());
            } catch (RuntimeException exception) {
                // Un fallo inesperado con un correo no frena al resto del lote.
                LOGGER.error("Unexpected error delivering email {}: {}", email.getId(), exception.getMessage(),
                        exception);
            }
        }
        return due.size();
    }
}
