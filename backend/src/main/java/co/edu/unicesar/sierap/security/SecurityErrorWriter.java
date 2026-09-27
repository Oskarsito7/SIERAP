package co.edu.unicesar.sierap.security;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Escribe los errores de seguridad (401 y 403) con el mismo formato
 * {@link ErrorResponse} que el resto de la API.
 *
 * <p>Hace falta porque esos errores ocurren en los filtros de Spring Security, antes
 * de llegar a los controllers, así que {@code GlobalExceptionHandler} no los ve.</p>
 */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Crea el escritor de errores.
     *
     * @param objectMapper serializador JSON de la aplicación
     * @param clock        reloj de la aplicación
     */
    public SecurityErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Escribe la respuesta de error.
     *
     * @param request  petición que falló
     * @param response respuesta HTTP
     * @param status   código HTTP (401 o 403)
     * @param message  mensaje en español para el usuario
     * @throws IOException si no se puede escribir la respuesta
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(clock), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
