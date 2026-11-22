package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import co.edu.unicesar.sierap.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link EmailService} sobre la cola de correos (ADR-022).
 *
 * <p>Encolar es insertar una fila en {@code email_outbox} dentro de la transacción de la
 * operación que llama: si esta se deshace, el correo desaparece. El envío real por SMTP
 * lo hace después el despachador ({@link EmailOutboxDispatcher}).</p>
 */
@Service
@Transactional
public class OutboxEmailService implements EmailService {

    private final EmailOutboxRepository outboxRepository;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param outboxRepository repositorio de la cola de correos
     * @param clock            reloj de la aplicación
     */
    public OutboxEmailService(EmailOutboxRepository outboxRepository, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.clock = clock;
    }

    @Override
    public void sendPasswordLink(User user, PasswordTokenPurpose purpose, String link, int validHours) {
        send(user.getEmail(), subjectFor(purpose), bodyFor(user, purpose, link, validHours));
    }

    @Override
    public void send(String to, String subject, String body) {
        outboxRepository.save(new EmailOutbox(to, subject, body, LocalDateTime.now(clock)));
    }

    private static String subjectFor(PasswordTokenPurpose purpose) {
        return switch (purpose) {
            case ACCOUNT_SETUP -> "SIERAP: crea tu contraseña";
            case PASSWORD_RESET -> "SIERAP: recupera tu contraseña";
        };
    }

    private static String bodyFor(User user, PasswordTokenPurpose purpose, String link, int validHours) {
        String reason = switch (purpose) {
            case ACCOUNT_SETUP -> "Se creó tu cuenta en SIERAP, el sistema de evaluación de los proyectos de aula "
                    + "y los RAP de la Universidad Popular del Cesar.\n\nPara crear tu contraseña";
            case PASSWORD_RESET -> "Recibimos una solicitud para recuperar la contraseña de tu cuenta en SIERAP."
                    + "\n\nPara crear una nueva";
        };
        String validity = validHours == 1 ? "1 hora" : validHours + " horas";
        return "Hola, " + user.getFirstNames() + ":\n\n"
                + reason + " entra a este enlace (vence en " + validity + "):\n\n"
                + link + "\n\n"
                + "Si no esperabas este correo, puedes ignorarlo.\n\n"
                + "SIERAP — Universidad Popular del Cesar";
    }
}
