package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado general de la API, usado por el frontend y por el equipo para verificar
 * que el backend está arriba y conectado a la base de datos.
 *
 * @param application       nombre de la aplicación
 * @param version           versión desplegada (tomada del {@code pom.xml})
 * @param profiles          perfiles de Spring en uso (p. ej. {@code dev})
 * @param databaseAvailable {@code true} si hay conexión con PostgreSQL
 * @param timestamp         fecha y hora del servidor en la zona horaria institucional
 */
public record SystemStatusResponse(
        String application,
        String version,
        List<String> profiles,
        boolean databaseAvailable,
        LocalDateTime timestamp) {
}
