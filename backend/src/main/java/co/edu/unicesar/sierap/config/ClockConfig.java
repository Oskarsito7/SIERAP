package co.edu.unicesar.sierap.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the system clock as an injectable bean.
 *
 * <p>Several business rules depend on the current date (submissions before the
 * deadline, grading inside the window set by the teacher and the UPC limit).
 * Services must use {@code LocalDate.now(clock)} instead of {@code LocalDate.now()}
 * so tests can pin "today" with {@link Clock#fixed} and the rules are always
 * evaluated in the configured time zone, not in the server one.</p>
 */
@Configuration
public class ClockConfig {

    /**
     * Creates the application clock in the institutional time zone.
     *
     * @param timeZone time zone identifier, e.g. {@code America/Bogota}
     * @return system clock in that time zone
     */
    @Bean
    public Clock clock(@Value("${sierap.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
