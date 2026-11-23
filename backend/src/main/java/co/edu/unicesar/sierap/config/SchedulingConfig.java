package co.edu.unicesar.sierap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el programador de tareas de Spring (ADR-022): el despachador de la cola de
 * correos y el trabajo diario de recordatorios.
 *
 * <p>Los beans programados se activan por propiedad ({@code sierap.mail.outbox.enabled} y
 * {@code sierap.reminders.enabled}); el perfil de pruebas los desactiva, de modo que
 * habilitar el programador aquí no hace correr nada en las pruebas.</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MailOutboxProperties.class)
public class SchedulingConfig {
}
