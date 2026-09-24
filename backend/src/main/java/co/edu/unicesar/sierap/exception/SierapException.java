package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception of every SIERAP business error.
 *
 * <p>Each subclass states, through polymorphism, the HTTP status the API must
 * respond with. {@link GlobalExceptionHandler} translates it into a uniform
 * response, so neither controllers nor services need {@code try/catch}: throwing
 * the right subclass is enough.</p>
 */
public abstract class SierapException extends RuntimeException {

    /**
     * Creates the exception with a message for the end user.
     *
     * @param message description of the error, in Spanish (it is shown to the user)
     */
    protected SierapException(String message) {
        super(message);
    }

    /**
     * Returns the HTTP status associated with this kind of error.
     *
     * @return HTTP status the API responds with
     */
    public abstract HttpStatus getHttpStatus();
}
