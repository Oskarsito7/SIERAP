package co.edu.unicesar.sierap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real PostgreSQL in Docker for the integration tests.
 *
 * <p>Thanks to {@link ServiceConnection}, Spring Boot points the datasource to the
 * container without extra properties. Tests therefore run against the same engine
 * and the same Flyway migrations as production (see ADR-007). Use with
 * {@code @Import(TestcontainersConfiguration.class)}.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Same image as {@code docker-compose.yml}, so tests do not run on another version. */
    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    /**
     * PostgreSQL container shared by the test context.
     *
     * @return container that Spring Boot starts and connects automatically
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));
    }
}
