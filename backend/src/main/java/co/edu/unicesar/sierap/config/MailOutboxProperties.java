package co.edu.unicesar.sierap.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de la cola de correos ({@code sierap.mail.outbox.*}, ADR-022).
 *
 * @param enabled       {@code false} desactiva el despachador (así no corre en las pruebas
 *                      de integración)
 * @param dispatchDelay espera entre el fin de una ronda del despachador y el inicio de la
 *                      siguiente
 * @param maxAttempts   intentos de envío antes de pasar el correo a {@code FAILED}
 * @param batchSize     correos que toma el despachador en cada ronda
 */
@ConfigurationProperties(prefix = "sierap.mail.outbox")
public record MailOutboxProperties(
        boolean enabled,
        Duration dispatchDelay,
        int maxAttempts,
        int batchSize) {

    /** Valores por defecto si alguna propiedad no se define. */
    public MailOutboxProperties {
        if (dispatchDelay == null) {
            dispatchDelay = Duration.ofSeconds(15);
        }
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (batchSize <= 0) {
            batchSize = 20;
        }
    }
}
