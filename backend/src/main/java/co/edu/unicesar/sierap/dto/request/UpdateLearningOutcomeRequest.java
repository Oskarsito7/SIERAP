package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Modificación parcial de un RAP en un momento en configuración.
 *
 * @param code        nuevo código del RAP, o {@code null} si se omite
 * @param description nueva descripción del RAP, o {@code null} si se omite
 */
public record UpdateLearningOutcomeRequest(
        @Size(max = 20, message = "El código admite hasta 20 caracteres")
        String code,

        @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
        String description) {
}
