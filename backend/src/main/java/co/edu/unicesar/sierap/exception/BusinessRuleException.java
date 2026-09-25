package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una operación viola una regla de negocio, p. ej. un grupo con
 * más integrantes de los permitidos o una entrega fuera de la fecha límite.
 * La API responde con {@code 422 Unprocessable Entity}.
 */
public class BusinessRuleException extends SierapException {

    /**
     * Crea la excepción con la descripción de la regla violada.
     *
     * @param message explicación de por qué no se permite la operación
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
