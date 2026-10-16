package co.edu.unicesar.sierap.security;

import co.edu.unicesar.sierap.entity.enums.UserRole;

/**
 * Usuario que hace la petición, tal como viene en el token de acceso.
 *
 * <p>Es el "principal" de Spring Security: los controllers lo reciben con
 * {@code @AuthenticationPrincipal} y se lo pasan a los services, que cargan la
 * entidad cuando necesitan más datos (p. ej. la facultad para validar el alcance).</p>
 *
 * @param id                 identificador del usuario
 * @param email              correo con el que inició sesión
 * @param role               rol del usuario
 * @param mustChangePassword {@code true} si debe cambiar su contraseña temporal
 */
public record AuthenticatedUser(Long id, String email, UserRole role, boolean mustChangePassword) {
}
