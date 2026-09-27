package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos para iniciar sesión.
 *
 * @param email correo de inicio de sesión
 * @param password contraseña
 */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
