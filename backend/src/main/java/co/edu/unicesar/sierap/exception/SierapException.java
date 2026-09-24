package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de todos los errores de negocio de SIERAP.
 *
 * <p>Cada subclase indica, por polimorfismo, el código HTTP con el que debe
 * responder la API. {@link GlobalExceptionHandler} la traduce a una respuesta
 * uniforme, de modo que ni los controllers ni los services necesitan
 * {@code try/catch}: basta con lanzar la subclase adecuada.</p>
 */
public abstract class SierapException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje legible por el usuario final.
     *
     * @param mensaje descripción del error, en español
     */
    protected SierapException(String mensaje) {
        super(mensaje);
    }

    /**
     * Indica el código HTTP asociado a este tipo de error.
     *
     * @return estado HTTP con el que responde la API
     */
    public abstract HttpStatus getEstadoHttp();
}
