package co.edu.unicesar.sierap.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import co.edu.unicesar.sierap.dto.response.EstadoSistemaResponse;
import co.edu.unicesar.sierap.service.EstadoSistemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link EstadoSistemaService}.
 *
 * <p>La verificación de la base de datos se hace directamente sobre el
 * {@link DataSource} (y no con un repository) porque lo que se comprueba es la
 * infraestructura, no una entidad del dominio.</p>
 */
@Service
public class EstadoSistemaServiceImpl implements EstadoSistemaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstadoSistemaServiceImpl.class);

    /** Segundos máximos que se espera a que PostgreSQL responda la validación. */
    private static final int SEGUNDOS_ESPERA_CONEXION = 2;

    private final DataSource dataSource;
    private final Clock reloj;
    private final Environment entorno;
    private final String nombreAplicacion;
    private final String version;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param dataSource       fuente de conexiones a PostgreSQL
     * @param reloj            reloj de la aplicación
     * @param entorno          entorno de Spring, para leer los perfiles activos
     * @param nombreAplicacion nombre configurado en {@code spring.application.name}
     * @param version          versión del artefacto, configurada en {@code sierap.version}
     */
    public EstadoSistemaServiceImpl(DataSource dataSource,
                                    Clock reloj,
                                    Environment entorno,
                                    @Value("${spring.application.name}") String nombreAplicacion,
                                    @Value("${sierap.version}") String version) {
        this.dataSource = dataSource;
        this.reloj = reloj;
        this.entorno = entorno;
        this.nombreAplicacion = nombreAplicacion;
        this.version = version;
    }

    @Override
    public EstadoSistemaResponse consultarEstado() {
        return new EstadoSistemaResponse(
                nombreAplicacion,
                version,
                perfilesEnUso(),
                baseDatosDisponible(),
                LocalDateTime.now(reloj));
    }

    /**
     * Perfiles activos o, si no se activó ninguno, los perfiles por defecto
     * ({@code spring.profiles.default}, que en SIERAP es {@code dev}).
     */
    private List<String> perfilesEnUso() {
        String[] activos = entorno.getActiveProfiles();
        return List.of(activos.length > 0 ? activos : entorno.getDefaultProfiles());
    }

    private boolean baseDatosDisponible() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(SEGUNDOS_ESPERA_CONEXION);
        } catch (SQLException excepcion) {
            LOGGER.warn("Sin conexión con la base de datos: {}", excepcion.getMessage());
            return false;
        }
    }
}
