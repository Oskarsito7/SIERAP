package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Título del proyecto de aula de un grupo.
 *
 * @param projectTitle título, hasta 200 caracteres
 */
public record ProjectTitleRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título admite hasta 200 caracteres") String projectTitle) {
}
