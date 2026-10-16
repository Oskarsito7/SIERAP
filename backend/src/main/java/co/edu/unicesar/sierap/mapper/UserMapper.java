package co.edu.unicesar.sierap.mapper;

import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
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
}
