package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Overall API status, used by the frontend and by the team to check that the
 * backend is up and connected to the database.
 *
 * @param application       application name
 * @param version           deployed version (taken from {@code pom.xml})
 * @param profiles          Spring profiles in use (e.g. {@code dev})
 * @param databaseAvailable {@code true} if there is a connection to PostgreSQL
 * @param timestamp         server date and time in the institutional time zone
 */
public record SystemStatusResponse(
        String application,
        String version,
        List<String> profiles,
        boolean databaseAvailable,
        LocalDateTime timestamp) {
}
