package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un usuario autenticado intenta acceder a un recurso fuera de
 * su alcance, p. ej. un docente consultando un grupo que no tiene asignado o un
 * admin modificando otra facultad. La API responde con {@code 403 Forbidden}
 * (nunca con un {@code 200} y una lista vacía).
 *
 * <p>No se llama {@code AccessDeniedException} para no chocar con la clase de
 * Spring Security del mismo nombre (Etapa 2).</p>
 */
public class ForbiddenAccessException extends SierapException {

    /**
     * Crea la excepción con la explicación del acceso denegado.
     *
     * @param message motivo por el que el usuario no puede acceder
     */
    public ForbiddenAccessException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
