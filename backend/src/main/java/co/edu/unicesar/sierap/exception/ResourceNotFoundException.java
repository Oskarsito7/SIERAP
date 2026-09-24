package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se solicita un recurso que no existe (una facultad, un grupo,
 * una entrega…). La API responde con {@code 404 Not Found}.
 */
public class ResourceNotFoundException extends SierapException {

    /**
     * Crea la excepción con un mensaje libre.
     *
     * @param message descripción del recurso no encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea la excepción a partir del tipo de recurso y su identificador,
     * p. ej. {@code new ResourceNotFoundException("Grupo", 7)} produce
     * "No existe Grupo con id 7".
     *
     * @param resource nombre del recurso buscado, tal como lo ve el usuario
     * @param id       identificador que no se encontró
     */
    public ResourceNotFoundException(String resource, Object id) {
        super("No existe " + resource + " con id " + id);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
