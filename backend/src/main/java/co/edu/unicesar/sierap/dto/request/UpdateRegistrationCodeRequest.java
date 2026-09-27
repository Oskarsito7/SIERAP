package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado de un código de registro.
 *
 * @param active {@code false} para desactivarlo
 */
public record UpdateRegistrationCodeRequest(
        @NotNull(message = "El estado es obligatorio") Boolean active) {
}
