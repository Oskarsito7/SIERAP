package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un criterio de evaluación en un RAP.
 *
 * @param description  descripción de qué se evalúa
 * @param displayOrder orden de presentación dentro del RAP (opcional; si se omite, se ubica al final)
 */
public record NewCriterionRequest(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String description,

        @Min(value = 1, message = "El orden de presentación debe ser al menos 1")
        Integer displayOrder) {
}
