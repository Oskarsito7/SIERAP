package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un parámetro global del sistema.
 *
 * @param value       valor del parámetro
 * @param description descripción opcional
 */
public record SetGlobalParameterRequest(
        @NotBlank(message = "El valor del parámetro es obligatorio")
        @Size(max = 500, message = "El valor no puede superar 500 caracteres")
        String value,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description) {
}
