package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;

/**
 * Correos de SIERAP (ADR-017, ADR-022). Enviar es encolar: cada método inserta una fila
 * en la cola de correos dentro de la transacción de quien llama, y un despachador
 * programado hace el envío real por SMTP con reintentos.
 */
public interface EmailService {

    /**
     * Encola el enlace para crear o recuperar la contraseña.
     *
     * <p>Se encola en la transacción actual: si esta se deshace, el correo desaparece. Si el
     * servidor de correo falla después, el despachador reintenta con espera exponencial.</p>
     *
     * @param user       destinatario
     * @param purpose    para qué sirve el enlace
     * @param link       enlace completo al frontend
     * @param validHours horas de vigencia del enlace, para mencionarlo en el correo
     */
    void sendPasswordLink(User user, PasswordTokenPurpose purpose, String link, int validHours);

    /**
     * Encola un correo de texto plano.
     *
     * <p>Misma política que {@link #sendPasswordLink}: se encola en la transacción actual y
     * el envío real y sus reintentos son asunto del despachador.</p>
     *
     * @param to      correo del destinatario
     * @param subject asunto
     * @param body    cuerpo en texto plano
     */
    void send(String to, String subject, String body);
}
