package co.edu.unicesar.sierap.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Fija el mismo semestre a varios estudiantes a la vez.
 *
 * @param studentIds      estudiantes a actualizar (de 1 a 500, sin repetidos)
 * @param currentSemester semestre que se les asigna
 */
public record SetSemesterRequest(
        @NotEmpty(message = "Seleccione al menos un estudiante")
        @Size(max = 500, message = "Se pueden actualizar hasta 500 estudiantes a la vez") List<@NotNull Long> studentIds,
        @NotNull(message = "El semestre es obligatorio")
        @Min(value = 1, message = "El semestre debe ser 1 o mayor") Integer currentSemester) {
}
