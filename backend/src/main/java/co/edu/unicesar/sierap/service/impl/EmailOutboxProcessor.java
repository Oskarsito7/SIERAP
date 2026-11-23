package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.config.MailOutboxProperties;
import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega un solo correo de la cola dentro de su propia transacción corta (ADR-022).
 *
 * <p>Vive en un bean aparte del despachador para que cada correo se confirme (o se
 * deshaga) por separado: un fallo no afecta a los demás del lote.</p>
 */
@Service
public class EmailOutboxProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOutboxProcessor.class);

    private final EmailOutboxRepository outboxRepository;
    private final SmtpMailSender mailSender;
    private final MailOutboxProperties properties;
    private final Clock clock;

    /**
     * Crea el procesador.
     *
     * @param outboxRepository repositorio de la cola de correos
     * @param mailSender       envío SMTP
     * @param properties       parámetros de la cola (intentos máximos)
     * @param clock            reloj de la aplicación
     */
    public EmailOutboxProcessor(EmailOutboxRepository outboxRepository, SmtpMailSender mailSender,
                                MailOutboxProperties properties, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.mailSender = mailSender;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Intenta enviar el correo. Si sale bien, lo marca {@code SENT}; si el servidor falla,
     * registra el intento y programa el reintento con espera exponencial, o lo pasa a
     * {@code FAILED} al agotar los intentos. Si el correo ya no está pendiente (otro
     * proceso lo tomó) no hace nada.
     *
     * @param id identificador del correo en la cola
     */
    @Transactional
    public void deliver(Long id) {
        EmailOutbox email = outboxRepository.findById(id)
                .filter(candidate -> candidate.getStatus() == EmailStatus.PENDING)
                .orElse(null);
        if (email == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            mailSender.send(email.getRecipient(), email.getSubject(), email.getBody());
            email.markSent(now);
        } catch (RuntimeException exception) {
            email.markFailedAttempt(now, String.valueOf(exception.getMessage()), properties.maxAttempts());
            if (email.getStatus() == EmailStatus.FAILED) {
                LOGGER.error("Email {} to {} failed for good after {} attempts: {}", id, email.getRecipient(),
                        email.getAttempts(), exception.getMessage());
            } else {
                LOGGER.warn("Email {} to {} failed (attempt {}), retrying at {}: {}", id, email.getRecipient(),
                        email.getAttempts(), email.getNextAttemptAt(), exception.getMessage());
            }
        }
        outboxRepository.save(email);
    }
}
