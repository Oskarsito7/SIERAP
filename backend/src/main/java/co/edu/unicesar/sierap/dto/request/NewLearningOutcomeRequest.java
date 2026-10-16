package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un nuevo RAP (Resultado de Aprendizaje del Programa) en un momento.
 *
 * @param code        código del RAP (p. ej. "RAP1")
 * @param description enunciado del resultado de aprendizaje
 */
public record NewLearningOutcomeRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código admite hasta 20 caracteres")
        String code,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
        String description) {
}
