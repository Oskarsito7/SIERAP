package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Petición para crear un nuevo programa académico.
 *
 * @param facultyId identificador de la facultad a la que pertenece
 * @param code      código institucional único (máximo 20 caracteres)
 * @param name      nombre del programa (máximo 150 caracteres)
 */
public record NewProgramRequest(
        @NotNull(message = "La facultad es obligatoria.")
        Long facultyId,

        @NotBlank(message = "El código del programa es obligatorio.")
        @Size(max = 20, message = "El código no puede superar los 20 caracteres.")
        String code,

        @NotBlank(message = "El nombre del programa es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String name) {
}
