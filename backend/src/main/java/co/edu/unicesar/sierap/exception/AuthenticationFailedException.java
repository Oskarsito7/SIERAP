package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando no se puede autenticar al usuario: credenciales incorrectas o un
 * token de renovación inválido, vencido o ya usado. La API responde con
 * {@code 401 Unauthorized}.
 */
public class AuthenticationFailedException extends SierapException {

    /**
     * Crea la excepción.
     *
     * @param message motivo, en español; en el login es siempre el mismo mensaje para
     *                no revelar si el correo existe
     */
    public AuthenticationFailedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
