package co.edu.unicesar.sierap.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone el reloj del sistema como un bean inyectable.
 *
 * <p>Varias reglas de negocio dependen de la fecha actual (entregas dentro de la
 * fecha límite, evaluación dentro de la ventana definida por el docente y del
 * rango fijado por la UPC). Los servicios deben usar {@code LocalDate.now(clock)}
 * en lugar de {@code LocalDate.now()}, para que las pruebas puedan fijar "hoy"
 * con {@link Clock#fixed} y las reglas se evalúen siempre en la zona horaria
 * configurada, no en la del servidor.</p>
 */
@Configuration
public class ClockConfig {

    /**
     * Crea el reloj de la aplicación en la zona horaria institucional.
     *
     * @param timeZone identificador de zona horaria, p. ej. {@code America/Bogota}
     * @return reloj del sistema en esa zona horaria
     */
    @Bean
    public Clock clock(@Value("${sierap.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
