package co.edu.unicesar.sierap.dto.response;

/**
 * Respuesta de inicio de sesión y de renovación: el token de acceso y el usuario.
 * El token de renovación no va aquí: viaja en una cookie {@code httpOnly} (ADR-017).
 *
 * @param accessToken JWT para el encabezado {@code Authorization: Bearer}
 * @param expiresIn   segundos de vigencia del token de acceso
 * @param user        usuario que inició sesión
 */
public record TokenResponse(String accessToken, long expiresIn, UserSummaryResponse user) {
}
