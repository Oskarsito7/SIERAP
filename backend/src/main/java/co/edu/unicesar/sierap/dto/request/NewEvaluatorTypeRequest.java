package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un tipo de evaluador externo en el catálogo.
 *
 * @param name        nombre visible único
 * @param description descripción opcional
 */
public record NewEvaluatorTypeRequest(
        @NotBlank(message = "El nombre del tipo de evaluador es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description) {
}
