package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Petición para modificar parcialmente un programa académico (la facultad no cambia).
 *
 * @param code nuevo código institucional (opcional)
 * @param name nuevo nombre del programa (opcional)
 */
public record UpdateProgramRequest(
        @Size(min = 1, max = 20, message = "El código debe tener entre 1 y 20 caracteres.")
        String code,

        @Size(min = 1, max = 150, message = "El nombre debe tener entre 1 y 150 caracteres.")
        String name) {
}
