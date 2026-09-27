package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado de una cuenta.
 *
 * @param active {@code false} para desactivar la cuenta
 */
public record UpdateUserRequest(
        @NotNull(message = "El estado es obligatorio") Boolean active) {
}
