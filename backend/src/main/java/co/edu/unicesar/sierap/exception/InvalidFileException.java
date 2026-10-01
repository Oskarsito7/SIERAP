package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un archivo subido no es del tipo aceptado (extensión, tipo de
 * contenido o firma del archivo) o llega vacío, p. ej. una entrega cuyo proyecto no
 * es un {@code .zip} (ADR-015). La API responde con {@code 400 Bad Request}.
 */
public class InvalidFileException extends SierapException {

    /**
     * Crea la excepción con la explicación del archivo rechazado.
     *
     * @param message qué archivo se rechazó y qué tipos se aceptan
     */
    public InvalidFileException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
