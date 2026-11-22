package co.edu.unicesar.sierap.dto.response;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;

/**
 * Resumen de un usuario que devuelve la API (nunca incluye la contraseña).
 *
 * @param id                 identificador
 * @param firstNames         nombres
 * @param lastNames          apellidos
 * @param fullName           nombre completo para mostrar
 * @param email              correo de inicio de sesión
 * @param role               rol
 * @param active             {@code true} si la cuenta puede iniciar sesión
 * @param mustChangePassword {@code true} si debe cambiar su contraseña temporal
 * @param documentType       tipo de documento de identidad; {@code null} si quien consulta no gestiona la cuenta
 * @param documentNumber     número de documento; {@code null} si quien consulta no gestiona la cuenta
 */
public record UserSummaryResponse(
        Long id,
        String firstNames,
        String lastNames,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        boolean mustChangePassword,
        DocumentType documentType,
        String documentNumber) {
}
