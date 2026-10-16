package co.edu.unicesar.sierap.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambios parciales de un grupo: solo se aplican los campos enviados (los que llegan
 * en {@code null} se dejan como están).
 *
 * @param name               nuevo nombre; solo antes del lanzamiento
 * @param submissionDeadline fecha límite de entrega, dentro de la ventana de entregas
 * @param evaluationDate     fecha de evaluación, dentro de la ventana de calificación
 */
public record UpdateGroupRequest(
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío") String name,
        LocalDate submissionDeadline,
        LocalDate evaluationDate) {
}
