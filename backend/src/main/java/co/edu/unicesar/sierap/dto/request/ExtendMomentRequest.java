package co.edu.unicesar.sierap.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Prórroga excepcional de un momento ya lanzado (ADR-023): amplía el fin de entregas
 * y/o el de calificación. Debe venir al menos una fecha y un motivo.
 *
 * @param submissionEnd nuevo último día de entregas, o {@code null} para no cambiarlo
 * @param gradingEnd    nuevo último día de calificación, o {@code null} para no cambiarlo
 * @param reason        motivo de la prórroga (10 a 500 caracteres)
 */
public record ExtendMomentRequest(
        LocalDate submissionEnd,
        LocalDate gradingEnd,
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres") String reason) {
}
