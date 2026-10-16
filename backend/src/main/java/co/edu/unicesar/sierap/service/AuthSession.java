package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.TokenResponse;

/**
 * Resultado de iniciar o renovar una sesión: lo que va en el cuerpo de la respuesta
 * y el valor del token de renovación, que el controller pone en la cookie.
 *
 * @param token        token de acceso y usuario, para el cuerpo de la respuesta
 * @param refreshToken valor del token de renovación, para la cookie {@code httpOnly}
 */
public record AuthSession(TokenResponse token, String refreshToken) {
}
