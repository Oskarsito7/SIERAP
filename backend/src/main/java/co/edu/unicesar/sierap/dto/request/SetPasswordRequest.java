package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Creación o recuperación de la contraseña con el enlace enviado por correo.
 *
 * @param token valor del enlace
 * @param newPassword nueva contraseña
 */
public record SetPasswordRequest(
        @NotBlank(message = "El enlace no es válido") String token,
        @NotBlank(message = "La nueva contraseña es obligatoria") @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword) {
}
