package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user tries to reach a resource outside their scope,
 * e.g. a teacher reading a group they did not enable or an admin changing another
 * faculty. The API responds with {@code 403 Forbidden} (never with a {@code 200}
 * and an empty list).
 *
 * <p>It is not called {@code AccessDeniedException} to avoid clashing with the
 * Spring Security class of that name (Stage 2).</p>
 */
public class ForbiddenAccessException extends SierapException {

    /**
     * Creates the exception with the reason access was denied.
     *
     * @param message why the user cannot access the resource, for the end user
     */
    public ForbiddenAccessException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
