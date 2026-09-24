package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform body of every error response of the API.
 *
 * @param timestamp   when the error happened
 * @param status      numeric HTTP status code (e.g. 404)
 * @param error       HTTP status reason phrase (e.g. "Not Found")
 * @param message     human-readable description of the error, shown to the end user
 * @param path        path of the request that failed
 * @param fieldErrors validation errors per field; empty when not applicable
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    /**
     * Creates an error response without per-field validation errors.
     *
     * @param timestamp when the error happened
     * @param status    numeric HTTP status code
     * @param error     HTTP status reason phrase
     * @param message   human-readable description of the error
     * @param path      path of the request that failed
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, Map.of());
    }
}
