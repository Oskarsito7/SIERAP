package co.edu.unicesar.sierap.dto.request;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un estudiante.
 *
 * @param firstNames nombres
 * @param lastNames apellidos
 * @param documentType tipo de documento de identidad
 * @param documentNumber número de documento
 * @param email correo de inicio de sesión
 * @param programId programa que cursa
 * @param studentCode código estudiantil
 * @param currentSemester semestre que cursa
 */
public record NewStudentRequest(
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String firstNames,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String lastNames,
        @NotNull(message = "El tipo de documento es obligatorio") DocumentType documentType,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 30) String documentNumber,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") @Size(max = 150) String email,
        @NotNull(message = "El programa es obligatorio") Long programId,
        @NotBlank(message = "El código estudiantil es obligatorio") @Size(max = 30) String studentCode,
        @NotNull(message = "El semestre es obligatorio") @Min(value = 1, message = "El semestre debe ser 1 o mayor") Integer currentSemester) {
}
