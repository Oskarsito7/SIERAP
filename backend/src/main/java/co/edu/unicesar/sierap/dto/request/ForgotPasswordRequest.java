package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud del enlace para recuperar una contraseña olvidada.
 *
 * @param email correo de la cuenta
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email) {
}
