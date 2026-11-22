package co.edu.unicesar.sierap.mapper;

import co.edu.unicesar.sierap.dto.response.StudentResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.entity.RegistrationCode;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.User;

/**
 * Convierte usuarios en su DTO de salida. Nunca expone el hash de la contraseña.
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Convierte un usuario (de cualquier rol) en su resumen, SIN su documento de
     * identidad: es lo que ve quien no gestiona la cuenta (p. ej. la búsqueda de un
     * evaluador externo por correo).
     *
     * @param user usuario
     * @return resumen del usuario con {@code documentType} y {@code documentNumber} nulos
     */
    public static UserSummaryResponse toSummary(User user) {
        return summary(user, false);
    }

    /**
     * Resumen con el documento de identidad, solo para la propia persona o para quien
     * gestiona su cuenta (lo necesita el formulario de edición).
     *
     * @param user usuario
     * @return resumen del usuario con su tipo y número de documento
     */
    public static UserSummaryResponse toSummaryWithDocument(User user) {
        return summary(user, true);
    }

    private static UserSummaryResponse summary(User user, boolean withDocument) {
        return new UserSummaryResponse(user.getId(), user.getFirstNames(), user.getLastNames(),
                user.getFullName(), user.getEmail(), user.getRole(), user.isActive(),
                user.isMustChangePassword(), withDocument ? user.getDocumentType() : null,
                withDocument ? user.getDocumentNumber() : null);
    }

    /**
     * Convierte un estudiante en el DTO de "Mis estudiantes" (DO-08).
     *
     * @param student    estudiante
     * @param manageable {@code true} si quien consulta puede gestionar su cuenta; solo
     *                   entonces se incluye el documento de identidad
     * @return estudiante con sus datos académicos y cómo llegó al sistema
     */
    public static StudentResponse toStudent(Student student, boolean manageable) {
        RegistrationCode code = student.getRegistrationCode();
        User creator = student.getCreatedBy();
        return new StudentResponse(student.getId(), student.getFirstNames(), student.getLastNames(),
                student.getFullName(), student.getEmail(), student.getRole(), student.isActive(),
                student.isMustChangePassword(), manageable ? student.getDocumentType() : null,
                manageable ? student.getDocumentNumber() : null,
                student.getStudentCode(), student.getCurrentSemester(),
                code == null ? null : code.getCode(),
                creator == null ? null : new StudentResponse.CreatorRef(creator.getId(), creator.getFullName()),
                manageable);
    }
}
