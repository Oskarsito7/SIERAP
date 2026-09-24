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
 * Manejo centralizado de excepciones de toda la API.
 *
 * <p>Traduce cualquier error a un {@link ErrorResponse} con el mismo formato:</p>
 * <ul>
 *   <li>{@link SierapException} y subclases → el código HTTP que define cada subclase.</li>
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

    private final Clock reloj;

    /**
     * Crea el manejador con el reloj de la aplicación.
     *
     * @param reloj reloj usado para fechar cada respuesta de error
     */
    public GlobalExceptionHandler(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Maneja los errores de negocio propios de SIERAP.
     *
     * @param excepcion error lanzado por un service
     * @param peticion  petición HTTP que lo originó
     * @return respuesta con el código HTTP que define la subclase
     */
    @ExceptionHandler(SierapException.class)
    public ResponseEntity<ErrorResponse> manejarSierapException(SierapException excepcion,
                                                                HttpServletRequest peticion) {
        HttpStatus estado = excepcion.getEstadoHttp();
        return ResponseEntity.status(estado)
                .body(crearCuerpo(estado, excepcion.getMessage(), peticion.getRequestURI(), Map.of()));
    }

    /**
     * Último recurso para errores no previstos: se registran completos en el log
     * y al cliente solo se le devuelve un mensaje genérico.
     *
     * @param excepcion error no controlado
     * @param peticion  petición HTTP que lo originó
     * @return respuesta {@code 500 Internal Server Error}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionInesperada(Exception excepcion,
                                                                    HttpServletRequest peticion) {
        LOGGER.error("Error inesperado procesando {}", peticion.getRequestURI(), excepcion);
        HttpStatus estado = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(estado)
                .body(crearCuerpo(estado, "Ocurrió un error inesperado. Intente de nuevo más tarde.",
                        peticion.getRequestURI(), Map.of()));
    }

    /**
     * Responde {@code 400} con el primer mensaje de validación de cada campo inválido.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excepcion,
                                                                  HttpHeaders cabeceras,
                                                                  HttpStatusCode estado,
                                                                  WebRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        excepcion.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(estado)
                .headers(cabeceras)
                .body(crearCuerpo(estado, "La solicitud tiene datos inválidos", obtenerRuta(peticion), errores));
    }

    /**
     * Punto único por el que pasan los demás errores de Spring MVC: reemplaza el
     * {@link ProblemDetail} por defecto por el formato {@link ErrorResponse} de SIERAP.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object cuerpo,
                                                          HttpHeaders cabeceras,
                                                          HttpStatusCode estado,
                                                          WebRequest peticion) {
        return ResponseEntity.status(estado)
                .headers(cabeceras)
                .body(crearCuerpo(estado, mensajePorEstado(estado, cuerpo), obtenerRuta(peticion), Map.of()));
    }

    private ErrorResponse crearCuerpo(HttpStatusCode estado, String mensaje, String ruta,
                                      Map<String, String> errores) {
        HttpStatus estadoConocido = HttpStatus.resolve(estado.value());
        String error = estadoConocido != null ? estadoConocido.getReasonPhrase() : "Error";
        return new ErrorResponse(LocalDateTime.now(reloj), estado.value(), error, mensaje, ruta, errores);
    }

    private static String mensajePorEstado(HttpStatusCode estado, Object cuerpo) {
        return switch (estado.value()) {
            case 400 -> "La solicitud no es válida";
            case 404 -> "La ruta solicitada no existe";
            case 405 -> "Método HTTP no permitido en esta ruta";
            case 415 -> "Tipo de contenido no soportado";
            default -> cuerpo instanceof ProblemDetail detalle && detalle.getDetail() != null
                    ? detalle.getDetail()
                    : "No se pudo procesar la solicitud";
        };
    }

    private static String obtenerRuta(WebRequest peticion) {
        return peticion instanceof ServletWebRequest peticionServlet
                ? peticionServlet.getRequest().getRequestURI()
                : "";
    }
}
