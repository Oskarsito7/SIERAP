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
     * Convierte un usuario (de cualquier rol) en su resumen.
     *
     * @param user usuario
     * @return resumen del usuario
     */
    public static UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getFirstNames(), user.getLastNames(),
                user.getFullName(), user.getEmail(), user.getRole(), user.isActive(),
                user.isMustChangePassword());
    }

    /**
     * Convierte un estudiante en el DTO de "Mis estudiantes" (DO-08).
     *
     * @param student    estudiante
     * @param manageable {@code true} si quien consulta puede gestionar su cuenta
     * @return estudiante con sus datos académicos y cómo llegó al sistema
     */
    public static StudentResponse toStudent(Student student, boolean manageable) {
        RegistrationCode code = student.getRegistrationCode();
        User creator = student.getCreatedBy();
        return new StudentResponse(student.getId(), student.getFirstNames(), student.getLastNames(),
                student.getFullName(), student.getEmail(), student.getRole(), student.isActive(),
                student.isMustChangePassword(), student.getStudentCode(), student.getCurrentSemester(),
                code == null ? null : code.getCode(),
                creator == null ? null : new StudentResponse.CreatorRef(creator.getId(), creator.getFullName()),
                manageable);
    }
}
