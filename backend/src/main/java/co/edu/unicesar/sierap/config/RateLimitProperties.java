package co.edu.unicesar.sierap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites de intentos por IP en las rutas de autenticación
 * ({@code sierap.security.rate-limit.*}, ADR-021).
 *
 * <p>Los límites se cuentan en una ventana fija por pareja (ruta, IP).</p>
 *
 * @param enabled       interruptor general; se desactiva solo en el perfil de pruebas
 * @param windowSeconds duración de la ventana, en segundos
 * @param login         intentos permitidos por ventana en {@code POST /api/auth/login}
 * @param register      intentos permitidos por ventana en {@code POST /api/auth/register}
 * @param forgot        intentos permitidos por ventana en {@code POST /api/auth/password/forgot}
 * @param set           intentos permitidos por ventana en {@code POST /api/auth/password/set}
 */
@ConfigurationProperties(prefix = "sierap.security.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int windowSeconds,
        int login,
        int register,
        int forgot,
        int set) {
}
