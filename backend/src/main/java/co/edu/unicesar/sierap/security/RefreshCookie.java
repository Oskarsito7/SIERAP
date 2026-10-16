package co.edu.unicesar.sierap.security;

import java.time.Duration;

import co.edu.unicesar.sierap.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye la cookie que guarda el token de renovación (ADR-017).
 *
 * <p>La cookie es {@code HttpOnly} (JavaScript no la puede leer), {@code SameSite=Strict}
 * (el navegador no la manda desde otros sitios) y solo viaja a {@code /api/auth}.
 * {@code Secure} se activa en producción, donde todo va por HTTPS.</p>
 */
@Component
public class RefreshCookie {

    /** Nombre de la cookie; el controller la lee con {@code @CookieValue}. */
    public static final String NAME = "sierap_refresh";

    private static final String PATH = "/api/auth";

    private final AuthProperties properties;

    /**
     * Crea la fábrica de cookies.
     *
     * @param properties parámetros de autenticación
     */
    public RefreshCookie(AuthProperties properties) {
        this.properties = properties;
    }

    /**
     * Cookie con un token de renovación nuevo, vigente lo mismo que el token.
     *
     * @param refreshToken valor del token de renovación
     * @return cookie para el encabezado {@code Set-Cookie}
     */
    public ResponseCookie create(String refreshToken) {
        return builder(refreshToken)
                .maxAge(Duration.ofDays(properties.refreshTokenDays()))
                .build();
    }

    /**
     * Cookie vacía y vencida, para que el navegador borre la actual al cerrar sesión.
     *
     * @return cookie para el encabezado {@code Set-Cookie}
     */
    public ResponseCookie clear() {
        return builder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
