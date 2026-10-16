package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;

/**
 * Envío de correos de SIERAP (ADR-017).
 */
public interface EmailService {

    /**
     * Envía el enlace para crear o recuperar la contraseña.
     *
     * <p>Si el servidor de correo falla, el error se registra en el log y la operación
     * que lo pidió no se deshace: quien creó la cuenta puede reenviar el enlace.</p>
     *
     * @param user       destinatario
     * @param purpose    para qué sirve el enlace
     * @param link       enlace completo al frontend
     * @param validHours horas de vigencia del enlace, para mencionarlo en el correo
     */
    void sendPasswordLink(User user, PasswordTokenPurpose purpose, String link, int validHours);
}
