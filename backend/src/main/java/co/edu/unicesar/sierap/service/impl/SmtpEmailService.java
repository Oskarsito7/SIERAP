package co.edu.unicesar.sierap.service.impl;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía los correos por SMTP (ADR-017): Mailpit en desarrollo, la cuenta
 * institucional de la UPC en producción.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    /**
     * Crea el servicio.
     *
     * @param mailSender cliente SMTP configurado por Spring Boot ({@code spring.mail.*})
     * @param properties parámetros de autenticación (remitente)
     */
    public SmtpEmailService(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordLink(User user, PasswordTokenPurpose purpose, String link, int validHours) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(user.getEmail());
        message.setSubject(subjectFor(purpose));
        message.setText(bodyFor(user, purpose, link, validHours));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // No se deshace la operación que pidió el correo: el enlace se puede reenviar.
            LOGGER.error("Could not send the {} email to {}: {}", purpose, user.getEmail(), exception.getMessage());
        }
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
