package co.edu.unicesar.sierap.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un periodo académico.
 *
 * @param code                  código único del periodo (ej. {@code 2026-2})
 * @param startDate             fecha de inicio del semestre
 * @param endDate               fecha de fin del semestre
 * @param upcEvaluationDeadline fecha límite institucional de evaluación UPC
 */
public record NewAcademicPeriodRequest(
        @NotBlank(message = "El código del periodo es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String code,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        @NotNull(message = "La fecha límite de evaluación UPC es obligatoria")
        LocalDate upcEvaluationDeadline) {
}
