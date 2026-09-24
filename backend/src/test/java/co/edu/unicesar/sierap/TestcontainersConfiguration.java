package co.edu.unicesar.sierap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta un PostgreSQL real en Docker para las pruebas de integración.
 *
 * <p>Gracias a {@link ServiceConnection}, Spring Boot configura el datasource
 * apuntando al contenedor, sin propiedades adicionales. Las pruebas corren así
 * contra el mismo motor y las mismas migraciones de Flyway que producción (ver
 * ADR-007). Usar con {@code @Import(TestcontainersConfiguration.class)}.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Misma imagen que {@code docker-compose.yml}, para no probar contra otra versión. */
    private static final String IMAGEN_POSTGRES = "postgres:17-alpine";

    /**
     * Contenedor de PostgreSQL compartido por el contexto de pruebas.
     *
     * @return contenedor que Spring Boot arranca y conecta automáticamente
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> contenedorPostgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse(IMAGEN_POSTGRES));
    }
}
