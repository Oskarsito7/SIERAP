package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.entity.User;

/**
 * Enlaces de un solo uso para crear o recuperar la contraseña (ADR-017).
 */
public interface PasswordLinkService {

    /**
     * Genera y envía el enlace para que un usuario nuevo cree su contraseña. Los
     * enlaces anteriores del mismo tipo dejan de servir.
     *
     * @param user usuario nuevo
     */
    void sendAccountSetupLink(User user);

    /**
     * Genera y envía el enlace para recuperar una contraseña olvidada. Los enlaces
     * anteriores del mismo tipo dejan de servir.
     *
     * @param user usuario que olvidó su contraseña
     */
    void sendPasswordResetLink(User user);

    /**
     * Valida un enlace, lo marca como usado y devuelve su usuario.
     *
     * @param token valor que llegó en el enlace
     * @return usuario dueño del enlace
     * @throws co.edu.unicesar.sierap.exception.BusinessRuleException si el enlace no
     *         existe, ya se usó o venció
     */
    User consume(String token);
}
