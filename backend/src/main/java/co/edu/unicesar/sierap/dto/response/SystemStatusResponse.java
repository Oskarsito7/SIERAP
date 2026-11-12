package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;

/**
 * Estado general de la API, usado por el frontend y por el equipo para verificar
 * que el backend está arriba y conectado a la base de datos. Es público, así que no
 * expone los perfiles de Spring en uso (auditoría de seguridad, run-1).
 *
 * @param application       nombre de la aplicación
 * @param version           versión desplegada (tomada del {@code pom.xml})
 * @param databaseAvailable {@code true} si hay conexión con PostgreSQL
 * @param timestamp         fecha y hora del servidor en la zona horaria institucional
 */
public record SystemStatusResponse(
        String application,
        String version,
        boolean databaseAvailable,
        LocalDateTime timestamp) {
}
