package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado general de la API, usado por el frontend y por el equipo para verificar
 * que el backend está arriba y conectado a la base de datos.
 *
 * @param aplicacion          nombre de la aplicación
 * @param version             versión desplegada (tomada del {@code pom.xml})
 * @param perfiles            perfiles de Spring en uso (p. ej. {@code dev})
 * @param baseDatosDisponible {@code true} si hay conexión con PostgreSQL
 * @param fechaHora           fecha y hora del servidor en la zona horaria institucional
 */
public record EstadoSistemaResponse(
        String aplicacion,
        String version,
        List<String> perfiles,
        boolean baseDatosDisponible,
        LocalDateTime fechaHora) {
}
