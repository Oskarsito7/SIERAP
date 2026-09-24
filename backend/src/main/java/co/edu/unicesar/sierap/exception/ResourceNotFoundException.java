package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a resource that does not exist is requested (a faculty, a group, a
 * submission…). The API responds with {@code 404 Not Found}.
 */
public class ResourceNotFoundException extends SierapException {

    /**
     * Creates the exception with a free-form message.
     *
     * @param message description of the missing resource, for the end user
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates the exception from the resource type and its identifier, e.g.
     * {@code new ResourceNotFoundException("Grupo", 7)} produces
     * "No existe Grupo con id 7".
     *
     * @param resource name of the resource as shown to the user (in Spanish)
     * @param id       identifier that was not found
     */
    public ResourceNotFoundException(String resource, Object id) {
        super("No existe " + resource + " con id " + id);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
