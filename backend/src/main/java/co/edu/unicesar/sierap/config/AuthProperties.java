package co.edu.unicesar.sierap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de autenticación y sesiones ({@code sierap.auth.*}, ADR-017).
 *
 * <p>Los valores sensibles (la clave de firma del JWT, el correo SMTP) llegan por
 * variables de entorno y nunca se guardan en el repositorio, salvo los valores de
 * desarrollo del perfil {@code dev}.</p>
 *
 * @param jwtSecret                clave para firmar los JWT con HS256 (mínimo 32 bytes)
 * @param accessTokenMinutes       vigencia del token de acceso, en minutos
 * @param refreshTokenDays         vigencia del token de renovación, en días
 * @param refreshCookieSecure      {@code true} para enviar la cookie solo por HTTPS
 * @param setupLinkHours           vigencia del enlace para crear la contraseña, en horas
 * @param resetLinkHours           vigencia del enlace para recuperar la contraseña, en horas
 * @param resetLinkCooldownMinutes minutos antes de enviar otro enlace de recuperación a la
 *                                 misma cuenta (evita inundar su correo)
 * @param frontendUrl              URL pública del frontend, base de los enlaces del correo
 * @param institutionalEmailDomain dominio exigido al registrarse con un código
 * @param mailFrom                 remitente de los correos
 */
@ConfigurationProperties(prefix = "sierap.auth")
public record AuthProperties(
        String jwtSecret,
        int accessTokenMinutes,
        int refreshTokenDays,
        boolean refreshCookieSecure,
        int setupLinkHours,
        int resetLinkHours,
        int resetLinkCooldownMinutes,
        String frontendUrl,
        String institutionalEmailDomain,
        String mailFrom) {
}
