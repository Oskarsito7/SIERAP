package co.edu.unicesar.sierap.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para generar un código de registro.
 *
 * @param programId programa en el que quedarán los estudiantes
 * @param expiresOn último día en que el código sirve
 * @param teacherId docente dueño del código; obligatorio solo si lo genera un admin o el super_admin
 */
public record NewRegistrationCodeRequest(
        @NotNull(message = "El programa es obligatorio") Long programId,
        @NotNull(message = "La fecha de vencimiento es obligatoria") LocalDate expiresOn,
        Long teacherId) {
}
