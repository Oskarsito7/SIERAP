package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio de contraseña de un usuario que ya inició sesión.
 *
 * @param currentPassword contraseña actual
 * @param newPassword nueva contraseña
 */
public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
        @NotBlank(message = "La nueva contraseña es obligatoria") @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword) {
}
