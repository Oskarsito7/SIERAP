package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición para crear una nueva facultad académica.
 *
 * @param code código institucional único (máximo 20 caracteres)
 * @param name nombre oficial de la facultad (máximo 150 caracteres)
 */
public record NewFacultyRequest(
        @NotBlank(message = "El código de la facultad es obligatorio.")
        @Size(max = 20, message = "El código no puede superar los 20 caracteres.")
        String code,

        @NotBlank(message = "El nombre de la facultad es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String name) {
}
