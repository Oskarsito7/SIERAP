package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo uniforme de todas las respuestas de error de la API.
 *
 * @param fechaHora momento en que ocurrió el error
 * @param estado    código HTTP numérico (p. ej. 404)
 * @param error     nombre del estado HTTP (p. ej. "Not Found")
 * @param mensaje   descripción legible del error
 * @param ruta      ruta de la petición que falló
 * @param errores   errores de validación por campo; vacío si no aplica
 */
public record ErrorResponse(
        LocalDateTime fechaHora,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> errores) {

    /**
     * Crea una respuesta de error sin errores de validación por campo.
     *
     * @param fechaHora momento en que ocurrió el error
     * @param estado    código HTTP numérico
     * @param error     nombre del estado HTTP
     * @param mensaje   descripción legible del error
     * @param ruta      ruta de la petición que falló
     */
    public ErrorResponse(LocalDateTime fechaHora, int estado, String error, String mensaje, String ruta) {
        this(fechaHora, estado, error, mensaje, ruta, Map.of());
    }
}
