package co.edu.unicesar.sierap.dto.request;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambios sobre una cuenta. Todos los campos son opcionales: solo se modifican los que
 * llegan. {@code studentCode} y {@code currentSemester} aplican únicamente a estudiantes.
 *
 * @param active          {@code false} para desactivar la cuenta, {@code true} para activarla
 * @param firstNames      nuevos nombres
 * @param lastNames       nuevos apellidos
 * @param documentType    nuevo tipo de documento
 * @param documentNumber  nuevo número de documento
 * @param email           nuevo correo de inicio de sesión
 * @param studentCode     nuevo código estudiantil (solo estudiantes)
 * @param currentSemester nuevo semestre (solo estudiantes)
 */
public record UpdateUserRequest(
        Boolean active,
        @Pattern(regexp = ".*\\S.*", message = "Los nombres no pueden estar vacíos")
        @Size(max = 100, message = "Los nombres no pueden pasar de 100 caracteres") String firstNames,
        @Pattern(regexp = ".*\\S.*", message = "Los apellidos no pueden estar vacíos")
        @Size(max = 100, message = "Los apellidos no pueden pasar de 100 caracteres") String lastNames,
        DocumentType documentType,
        @Pattern(regexp = ".*\\S.*", message = "El número de documento no puede estar vacío")
        @Size(max = 30, message = "El número de documento no puede pasar de 30 caracteres") String documentNumber,
        @Pattern(regexp = ".*\\S.*", message = "El correo no puede estar vacío")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede pasar de 150 caracteres") String email,
        @Pattern(regexp = ".*\\S.*", message = "El código estudiantil no puede estar vacío")
        @Size(max = 30, message = "El código estudiantil no puede pasar de 30 caracteres") String studentCode,
        @Min(value = 1, message = "El semestre debe ser 1 o mayor") Integer currentSemester) {

    /** @return {@code true} si el cuerpo no trae ningún campo para cambiar */
    public boolean isEmpty() {
        return active == null && firstNames == null && lastNames == null && documentType == null
                && documentNumber == null && email == null && studentCode == null && currentSemester == null;
    }
}
