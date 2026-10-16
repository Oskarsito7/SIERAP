package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para que un docente habilite un grupo en un momento.
 *
 * @param momentId momento en el que se habilita
 * @param name     nombre visible del grupo
 */
public record NewGroupRequest(
        @NotNull(message = "El momento es obligatorio") Long momentId,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres") String name) {
}
