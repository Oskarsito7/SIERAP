package co.edu.unicesar.sierap.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import co.edu.unicesar.sierap.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita los intentos por IP en las rutas de autenticación sensibles (ADR-021).
 *
 * <p>Usa una ventana fija en memoria por pareja (ruta, IP): cuando se supera el límite
 * de la ruta responde {@code 429} con el formato uniforme de error y la cabecera
 * {@code Retry-After} con los segundos que faltan. Solo cuenta las peticiones
 * {@code POST} a login, registro y recuperación o creación de contraseña; el resto de
 * la API no se ve afectado.</p>
 *
 * <p>La IP es {@code request.getRemoteAddr()}. Detrás de un proxy inverso hay que
 * activar {@code server.forward-headers-strategy: native} (ya está en el perfil
 * {@code prod}); si no, todas las peticiones parecerían venir del proxy. El estado es
 * local a la instancia: con varias réplicas cada una lleva su propia cuenta.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Mensaje que ve el usuario al superar el límite. */
    static final String MESSAGE = "Demasiados intentos. Espera un momento e inténtalo de nuevo.";

    private final RateLimitProperties properties;
    private final SecurityErrorWriter errorWriter;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Crea el filtro.
     *
     * @param properties  límites configurados
     * @param errorWriter escritor de errores con el formato de la API
     * @param clock       reloj de la aplicación
     */
    public RateLimitFilter(RateLimitProperties properties, SecurityErrorWriter errorWriter, Clock clock) {
        this.properties = properties;
        this.errorWriter = errorWriter;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limitFor(request);
        if (!properties.enabled() || limit <= 0) {
            chain.doFilter(request, response);
            return;
        }
        Instant now = clock.instant();
        Duration length = Duration.ofSeconds(properties.windowSeconds());
        removeExpired(now, length);
        String key = pathOf(request) + "|" + request.getRemoteAddr();
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || !now.isBefore(current.start().plus(length))) {
                return new Window(now, 1);
            }
            return new Window(current.start(), current.count() + 1);
        });
        if (window.count() > limit) {
            long remaining = Math.max(1, Duration.between(now, window.start().plus(length)).toSeconds());
            response.setHeader("Retry-After", String.valueOf(remaining));
            errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, MESSAGE);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Intentos permitidos para la petición, o 0 si la ruta no está limitada. */
    private int limitFor(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return 0;
        }
        return switch (pathOf(request)) {
            case "/api/auth/login" -> properties.login();
            case "/api/auth/register" -> properties.register();
            case "/api/auth/password/forgot" -> properties.forgot();
            case "/api/auth/password/set" -> properties.set();
            default -> 0;
        };
    }

    /** Descarta las ventanas vencidas para que el mapa no crezca sin límite. */
    /**
     * Ruta ya decodificada y normalizada por el contenedor (servlet path + path info). No se
     * usa {@code getRequestURI()} porque llega sin decodificar: {@code /api/auth/%6Cogin}
     * no coincidiría con {@code /api/auth/login}, pero Spring MVC sí la atiende como login,
     * y así se podría saltar el límite.
     */
    private static String pathOf(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }

    private void removeExpired(Instant now, Duration length) {
        windows.values().removeIf(window -> !now.isBefore(window.start().plus(length)));
    }

    /** Ventana de conteo: instante de inicio y número de intentos. */
    private record Window(Instant start, int count) {
    }
}
