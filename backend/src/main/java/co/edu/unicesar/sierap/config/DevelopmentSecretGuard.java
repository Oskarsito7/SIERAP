package co.edu.unicesar.sierap.config;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Impide que la API arranque firmando los JWT con la clave pública de desarrollo
 * cuando el perfil {@code dev} no se activó de forma explícita (auditoría de
 * seguridad, run-1).
 *
 * <p>El perfil por defecto es {@code dev} para que el equipo trabaje sin configurar
 * nada, pero esa clave está en el repositorio: si un despliegue arrancara sin perfil y
 * sin {@code SIERAP_JWT_SECRET}, cualquiera podría forjar un token de super_admin. Con
 * este control, la clave de desarrollo solo sirve si {@code dev} está en los perfiles
 * activos ({@code ./mvnw spring-boot:run} lo activa solo). En cualquier otro caso la
 * aplicación se detiene y explica qué falta.</p>
 */
@Component
public class DevelopmentSecretGuard {

    /** Nombre del perfil de desarrollo. */
    static final String DEV_PROFILE = "dev";

    /** Propiedad con la clave pública de desarrollo (solo existe en el perfil dev). */
    static final String DEVELOPMENT_SECRET_PROPERTY = "sierap.development-jwt-secret";

    /**
     * Revisa la configuración al crear el bean.
     *
     * @param environment entorno de Spring, para leer perfiles y propiedades
     * @param properties  parámetros de autenticación ya resueltos
     * @throws IllegalStateException si se usaría la clave de desarrollo sin activar dev
     */
    public DevelopmentSecretGuard(Environment environment, AuthProperties properties) {
        String developmentSecret = environment.getProperty(DEVELOPMENT_SECRET_PROPERTY);
        boolean usesDevelopmentSecret = developmentSecret != null
                && developmentSecret.equals(properties.jwtSecret());
        boolean devExplicitlyActive = Arrays.asList(environment.getActiveProfiles()).contains(DEV_PROFILE);
        if (usesDevelopmentSecret && !devExplicitlyActive) {
            throw new IllegalStateException("La API iba a firmar los JWT con la clave pública de desarrollo. "
                    + "En producción arranque con --spring.profiles.active=prod y SIERAP_JWT_SECRET; "
                    + "en local active el perfil dev (./mvnw spring-boot:run ya lo hace).");
        }
    }
}
