package co.edu.unicesar.sierap.exception;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import co.edu.unicesar.sierap.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralized exception handling for the whole API.
 *
 * <p>Translates every error into an {@link ErrorResponse} with the same format:</p>
 * <ul>
 *   <li>{@link SierapException} and subclasses → the HTTP status each subclass defines.</li>
 *   <li>{@code @Valid} validation errors → {@code 400} with the detail per field.</li>
 *   <li>Spring MVC errors (unknown path, method not allowed, malformed JSON…) →
 *       their standard HTTP status, inherited from
 *       {@link ResponseEntityExceptionHandler}.</li>
 *   <li>Any other exception → {@code 500}, without exposing internal details.</li>
 * </ul>
 *
 * <p>The messages in the responses are shown to the end user, so they are in
 * Spanish.</p>
 *
 * <p>Note for Stage 2: Spring Security exceptions thrown inside a controller (e.g.
 * {@code AccessDeniedException} from {@code @PreAuthorize}) need their own
 * {@code @ExceptionHandler}; otherwise they fall into the generic handler and
 * respond {@code 500} instead of {@code 403}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    /**
     * Creates the handler with the application clock.
     *
     * @param clock clock used to timestamp every error response
     */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Handles SIERAP business errors.
     *
     * @param exception error thrown by a service
     * @param request   HTTP request that caused it
     * @return response with the HTTP status defined by the subclass
     */
    @ExceptionHandler(SierapException.class)
    public ResponseEntity<ErrorResponse> handleSierapException(SierapException exception,
                                                               HttpServletRequest request) {
        HttpStatus status = exception.getHttpStatus();
        return ResponseEntity.status(status)
                .body(buildBody(status, exception.getMessage(), request.getRequestURI(), Map.of()));
    }

    /**
     * Last resort for unexpected errors: they are logged in full and the client
     * only gets a generic message.
     *
     * @param exception unhandled error
     * @param request   HTTP request that caused it
     * @return {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception,
                                                                   HttpServletRequest request) {
        LOGGER.error("Unexpected error processing {}", request.getRequestURI(), exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(buildBody(status, "Ocurrió un error inesperado. Intente de nuevo más tarde.",
                        request.getRequestURI(), Map.of()));
    }

    /**
     * Responds {@code 400} with the first validation message of each invalid field.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(status)
                .headers(headers)
                .body(buildBody(status, "La solicitud tiene datos inválidos", pathOf(request), fieldErrors));
    }

    /**
     * Single point every other Spring MVC error goes through: replaces the default
     * {@link ProblemDetail} with the SIERAP {@link ErrorResponse} format.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body,
                                                          HttpHeaders headers,
                                                          HttpStatusCode status,
                                                          WebRequest request) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(buildBody(status, messageForStatus(status, body), pathOf(request), Map.of()));
    }

    private ErrorResponse buildBody(HttpStatusCode status, String message, String path,
                                    Map<String, String> fieldErrors) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String error = knownStatus != null ? knownStatus.getReasonPhrase() : "Error";
        return new ErrorResponse(LocalDateTime.now(clock), status.value(), error, message, path, fieldErrors);
    }

    private static String messageForStatus(HttpStatusCode status, Object body) {
        return switch (status.value()) {
            case 400 -> "La solicitud no es válida";
            case 404 -> "La ruta solicitada no existe";
            case 405 -> "Método HTTP no permitido en esta ruta";
            case 415 -> "Tipo de contenido no soportado";
            default -> body instanceof ProblemDetail detail && detail.getDetail() != null
                    ? detail.getDetail()
                    : "No se pudo procesar la solicitud";
        };
    }

    private static String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";
    }
}
