package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un usuario autenticado intenta acceder a un recurso fuera de
 * su alcance, p. ej. un docente consultando un grupo que no tiene asignado o un
 * admin modificando otra facultad. La API responde con {@code 403 Forbidden}
 * (nunca con un {@code 200} y una lista vacía).
 */
public class AccesoDenegadoException extends SierapException {

    /**
     * Crea la excepción con la explicación del acceso denegado.
     *
     * @param mensaje motivo por el que el usuario no puede acceder
     */
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus getEstadoHttp() {
        return HttpStatus.FORBIDDEN;
    }
}
