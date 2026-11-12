package co.edu.unicesar.sierap.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;
import co.edu.unicesar.sierap.service.SystemStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link SystemStatusService}.
 *
 * <p>La verificación de la base de datos se hace directamente sobre el
 * {@link DataSource} (y no con un repository) porque lo que se comprueba es la
 * infraestructura, no una entidad del dominio.</p>
 */
@Service
public class SystemStatusServiceImpl implements SystemStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemStatusServiceImpl.class);

    /** Segundos máximos que se espera a que PostgreSQL responda la validación. */
    private static final int CONNECTION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final Clock clock;
    private final String applicationName;
    private final String version;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param dataSource      fuente de conexiones a PostgreSQL
     * @param clock           reloj de la aplicación
     * @param applicationName nombre configurado en {@code spring.application.name}
     * @param version         versión del artefacto, configurada en {@code sierap.version}
     */
    public SystemStatusServiceImpl(DataSource dataSource,
                                   Clock clock,
                                   @Value("${spring.application.name}") String applicationName,
                                   @Value("${sierap.version}") String version) {
        this.dataSource = dataSource;
        this.clock = clock;
        this.applicationName = applicationName;
        this.version = version;
    }

    @Override
    public SystemStatusResponse getStatus() {
        return new SystemStatusResponse(
                applicationName,
                version,
                isDatabaseAvailable(),
                LocalDateTime.now(clock));
    }

    private boolean isDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(CONNECTION_TIMEOUT_SECONDS);
        } catch (SQLException exception) {
            LOGGER.warn("No database connection: {}", exception.getMessage());
            return false;
        }
    }
}
