package co.edu.unicesar.sierap.dto.request;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un evaluador externo (empresario, egresado u otro tipo del catálogo).
 *
 * @param firstNames nombres
 * @param lastNames apellidos
 * @param documentType tipo de documento de identidad
 * @param documentNumber número de documento
 * @param email correo de inicio de sesión
 * @param evaluatorTypeId tipo de evaluador del catálogo
 * @param organization organización (opcional)
 * @param position cargo (opcional)
 * @param graduationYear año de graduación (opcional)
 * @param graduationProgramId programa de egreso (opcional)
 */
public record NewExternalEvaluatorRequest(
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String firstNames,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String lastNames,
        @NotNull(message = "El tipo de documento es obligatorio") DocumentType documentType,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 30) String documentNumber,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") @Size(max = 150) String email,
        @NotNull(message = "El tipo de evaluador es obligatorio") Long evaluatorTypeId,
        @Size(max = 150) String organization,
        @Size(max = 100) String position,
        Integer graduationYear,
        Long graduationProgramId) {
}
