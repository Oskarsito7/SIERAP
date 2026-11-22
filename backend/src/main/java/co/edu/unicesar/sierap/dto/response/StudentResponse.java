package co.edu.unicesar.sierap.dto.response;

import co.edu.unicesar.sierap.entity.enums.UserRole;

/**
 * Estudiante tal como lo ven la lista "Mis estudiantes" (DO-08) y la respuesta al
 * crearlo: el resumen de la cuenta más sus datos académicos, cómo llegó al sistema y si
 * quien consulta puede gestionarlo.
 *
 * <p>{@code manageable} solo sirve para que la pantalla oculte las acciones que no
 * puede usar: el backend vuelve a validar el alcance en cada operación (ADR-017).</p>
 *
 * @param id                 identificador
 * @param firstNames         nombres
 * @param lastNames          apellidos
 * @param fullName           nombre completo para mostrar
 * @param email              correo de inicio de sesión
 * @param role               rol (siempre {@code STUDENT})
 * @param active             {@code true} si la cuenta puede iniciar sesión
 * @param mustChangePassword {@code true} si debe cambiar su contraseña temporal
 * @param studentCode        código estudiantil
 * @param currentSemester    semestre que cursa
 * @param registrationCode   código de registro con el que se registró, o {@code null} si
 *                           alguien creó su cuenta
 * @param createdBy          quien creó la cuenta o, si se registró con un código, el
 *                           docente dueño del código; {@code null} si no se sabe
 * @param manageable         {@code true} si quien consulta puede reenviarle el enlace o
 *                           activar y desactivar su cuenta
 */
public record StudentResponse(
        Long id,
        String firstNames,
        String lastNames,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        boolean mustChangePassword,
        String studentCode,
        int currentSemester,
        String registrationCode,
        CreatorRef createdBy,
        boolean manageable) {

    /**
     * Quien creó la cuenta del estudiante.
     *
     * @param id       identificador del usuario
     * @param fullName nombre completo
     */
    public record CreatorRef(Long id, String fullName) {
    }
}
