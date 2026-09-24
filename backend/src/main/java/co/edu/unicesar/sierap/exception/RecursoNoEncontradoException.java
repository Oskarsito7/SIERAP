package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se solicita un recurso que no existe (una facultad, un grupo,
 * una entrega…). La API responde con {@code 404 Not Found}.
 */
public class RecursoNoEncontradoException extends SierapException {

    /**
     * Crea la excepción con un mensaje libre.
     *
     * @param mensaje descripción del recurso no encontrado
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción a partir del tipo de recurso y su identificador,
     * p. ej. {@code new RecursoNoEncontradoException("Grupo", 7)} produce
     * "No existe Grupo con id 7".
     *
     * @param recurso nombre del recurso buscado
     * @param id      identificador que no se encontró
     */
    public RecursoNoEncontradoException(String recurso, Object id) {
        super("No existe " + recurso + " con id " + id);
    }

    @Override
    public HttpStatus getEstadoHttp() {
        return HttpStatus.NOT_FOUND;
    }
}
