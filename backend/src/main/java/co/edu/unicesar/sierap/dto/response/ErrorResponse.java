package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo uniforme de todas las respuestas de error de la API.
 *
 * @param timestamp   momento en que ocurrió el error
 * @param status      código HTTP numérico (p. ej. 404)
 * @param error       nombre del estado HTTP (p. ej. "Not Found")
 * @param message     descripción legible del error, en español (la ve el usuario)
 * @param path        ruta de la petición que falló
 * @param fieldErrors errores de validación por campo; vacío si no aplica
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    /**
     * Crea una respuesta de error sin errores de validación por campo.
     *
     * @param timestamp momento en que ocurrió el error
     * @param status    código HTTP numérico
     * @param error     nombre del estado HTTP
     * @param message   descripción legible del error
     * @param path      ruta de la petición que falló
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, Map.of());
    }
}
