package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Modificación parcial de un criterio de evaluación.
 *
 * @param description  nueva descripción del criterio, o {@code null} si se omite
 * @param displayOrder nuevo orden de presentación dentro del RAP, o {@code null} si se omite
 */
public record UpdateCriterionRequest(
        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String description,

        @Min(value = 1, message = "El orden de presentación debe ser al menos 1")
        Integer displayOrder) {
}
