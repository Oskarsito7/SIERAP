package co.edu.unicesar.sierap.service.impl;

import co.edu.unicesar.sierap.config.AuthProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envío real de un correo por SMTP (ADR-017): Mailpit en desarrollo, la cuenta
 * institucional de la UPC en producción.
 *
 * <p>Solo lo usa el despachador de la cola ({@link EmailOutboxProcessor}); el resto del
 * sistema encola con {@code EmailService}. Si el servidor falla, la excepción sube para
 * que el despachador registre el intento fallido y reintente más tarde.</p>
 */
@Component
public class SmtpMailSender {

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    /**
     * Crea el componente.
     *
     * @param mailSender cliente SMTP configurado por Spring Boot ({@code spring.mail.*})
     * @param properties parámetros de autenticación (remitente)
     */
    public SmtpMailSender(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * Envía un correo de texto plano.
     *
     * @param to      destinatario
     * @param subject asunto
     * @param body    cuerpo en texto plano
     * @throws MailException si el servidor SMTP no lo acepta
     */
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
