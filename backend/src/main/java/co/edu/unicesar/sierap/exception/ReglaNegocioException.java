package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una operación viola una regla de negocio, p. ej. un grupo con
 * más integrantes de los permitidos o una entrega fuera de la fecha límite.
 * La API responde con {@code 422 Unprocessable Entity}.
 */
public class ReglaNegocioException extends SierapException {

    /**
     * Crea la excepción con la descripción de la regla violada.
     *
     * @param mensaje explicación de por qué no se permite la operación
     */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus getEstadoHttp() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
