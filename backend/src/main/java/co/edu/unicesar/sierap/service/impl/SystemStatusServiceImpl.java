package co.edu.unicesar.sierap.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;
import co.edu.unicesar.sierap.service.SystemStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link SystemStatusService}.
 *
 * <p>The database check runs directly on the {@link DataSource} (not through a
 * repository) because what is checked is the infrastructure, not a domain
 * entity.</p>
 */
@Service
public class SystemStatusServiceImpl implements SystemStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemStatusServiceImpl.class);

    /** Maximum seconds to wait for PostgreSQL to answer the validation. */
    private static final int CONNECTION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final Clock clock;
    private final Environment environment;
    private final String applicationName;
    private final String version;

    /**
     * Creates the service with its dependencies.
     *
     * @param dataSource      PostgreSQL connection source
     * @param clock           application clock
     * @param environment     Spring environment, to read the active profiles
     * @param applicationName name configured in {@code spring.application.name}
     * @param version         artifact version, configured in {@code sierap.version}
     */
    public SystemStatusServiceImpl(DataSource dataSource,
                                   Clock clock,
                                   Environment environment,
                                   @Value("${spring.application.name}") String applicationName,
                                   @Value("${sierap.version}") String version) {
        this.dataSource = dataSource;
        this.clock = clock;
        this.environment = environment;
        this.applicationName = applicationName;
        this.version = version;
    }

    @Override
    public SystemStatusResponse getStatus() {
        return new SystemStatusResponse(
                applicationName,
                version,
                profilesInUse(),
                isDatabaseAvailable(),
                LocalDateTime.now(clock));
    }

    /**
     * Active profiles or, when none was activated, the default profiles
     * ({@code spring.profiles.default}, which is {@code dev} in SIERAP).
     */
    private List<String> profilesInUse() {
        String[] active = environment.getActiveProfiles();
        return List.of(active.length > 0 ? active : environment.getDefaultProfiles());
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
