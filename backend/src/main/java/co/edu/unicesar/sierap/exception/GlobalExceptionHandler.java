package co.edu.unicesar.sierap.exception;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import co.edu.unicesar.sierap.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
 * Manejo centralizado de excepciones de toda la API.
 *
 * <p>Traduce cualquier error a un {@link ErrorResponse} con el mismo formato:</p>
 * <ul>
 *   <li>{@link SierapException} y subclases → el código HTTP que define cada subclase.</li>
 *   <li>{@link DataIntegrityViolationException} (la base de datos rechazó la operación
 *       por una llave foránea {@code RESTRICT}, un único o un {@code CHECK}) →
 *       {@code 409}, con un mensaje genérico en español (ADR-016).</li>
 *   <li>Errores de validación de {@code @Valid} → {@code 400} con el detalle por campo.</li>
 *   <li>Errores propios de Spring MVC (ruta inexistente, método no permitido, JSON
 *       mal formado…) → su código HTTP estándar, heredado de
 *       {@link ResponseEntityExceptionHandler}.</li>
 *   <li>Cualquier otra excepción → {@code 500}, sin exponer detalles internos.</li>
 * </ul>
 *
 * <p>Nota para la Etapa 2: las excepciones de Spring Security lanzadas dentro de
 * un controller (p. ej. {@code AccessDeniedException} por {@code @PreAuthorize})
 * necesitan su propio {@code @ExceptionHandler}; si no, caen en el manejador
 * genérico y responden {@code 500} en lugar de {@code 403}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    /**
     * Crea el manejador con el reloj de la aplicación.
     *
     * @param clock reloj usado para fechar cada respuesta de error
     */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Maneja los errores de negocio propios de SIERAP.
     *
     * @param exception error lanzado por un service
     * @param request   petición HTTP que lo originó
     * @return respuesta con el código HTTP que define la subclase
     */
    @ExceptionHandler(SierapException.class)
    public ResponseEntity<ErrorResponse> handleSierapException(SierapException exception,
                                                               HttpServletRequest request) {
        HttpStatus status = exception.getHttpStatus();
        return ResponseEntity.status(status)
                .body(buildBody(status, exception.getMessage(), request.getRequestURI(), Map.of()));
    }

    /**
     * Maneja las operaciones que PostgreSQL rechaza por integridad de datos: borrar
     * configuración que ya tiene grupos o notas, registros duplicados o valores que
     * violan un {@code CHECK} (ADR-016).
     *
     * <p>Es la última línea de defensa: los services deben validar antes las reglas
     * de negocio y lanzar una {@link BusinessRuleException} con un mensaje preciso.
     * El detalle técnico solo se registra en el log.</p>
     *
     * @param exception error de integridad traducido por Spring
     * @param request   petición HTTP que lo originó
     * @return respuesta {@code 409 Conflict}
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception,
                                                                      HttpServletRequest request) {
        LOGGER.warn("Data integrity violation processing {}: {}", request.getRequestURI(),
                exception.getMostSpecificCause().getMessage());
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status)
                .body(buildBody(status,
                        "No se puede completar la operación porque entra en conflicto con información "
                                + "existente (datos duplicados o relacionados).",
                        request.getRequestURI(), Map.of()));
    }

    /**
     * Último recurso para errores no previstos: se registran completos en el log
     * y al cliente solo se le devuelve un mensaje genérico.
     *
     * @param exception error no controlado
     * @param request   petición HTTP que lo originó
     * @return respuesta {@code 500 Internal Server Error}
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
     * Responde {@code 400} con el primer mensaje de validación de cada campo inválido.
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
     * Punto único por el que pasan los demás errores de Spring MVC: reemplaza el
     * {@link ProblemDetail} por defecto por el formato {@link ErrorResponse} de SIERAP.
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
