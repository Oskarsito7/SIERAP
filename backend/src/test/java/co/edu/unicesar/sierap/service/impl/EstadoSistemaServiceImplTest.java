package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.sql.DataSource;

import co.edu.unicesar.sierap.dto.response.EstadoSistemaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * Pruebas unitarias de {@link EstadoSistemaServiceImpl}, con la base de datos
 * simulada (Mockito) y el reloj fijo.
 */
@ExtendWith(MockitoExtension.class)
class EstadoSistemaServiceImplTest {

    /** 14:30 UTC equivale a las 09:30 en Bogotá (UTC-5). */
    private static final Clock RELOJ_FIJO =
            Clock.fixed(Instant.parse("2026-03-15T14:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection conexion;

    private MockEnvironment entorno;

    private EstadoSistemaServiceImpl servicio;

    @BeforeEach
    void configurar() {
        entorno = new MockEnvironment();
        entorno.setActiveProfiles("prod");
        servicio = new EstadoSistemaServiceImpl(dataSource, RELOJ_FIJO, entorno, "sierap", "0.1.0");
    }

    @Test
    void reportaBaseDeDatosDisponibleCuandoLaConexionEsValida() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(anyInt())).thenReturn(true);

        EstadoSistemaResponse estado = servicio.consultarEstado();

        assertThat(estado.baseDatosDisponible()).isTrue();
        assertThat(estado.aplicacion()).isEqualTo("sierap");
        assertThat(estado.version()).isEqualTo("0.1.0");
        assertThat(estado.perfiles()).containsExactly("prod");
        assertThat(estado.fechaHora()).isEqualTo(LocalDateTime.of(2026, 3, 15, 9, 30));
        verify(conexion).close();
    }

    @Test
    void reportaLosPerfilesPorDefectoCuandoNoHayPerfilesActivos() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        entorno.setActiveProfiles();
        entorno.setDefaultProfiles("dev");

        assertThat(servicio.consultarEstado().perfiles()).containsExactly("dev");
    }

    @Test
    void reportaBaseDeDatosNoDisponibleCuandoFallaLaConexion() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        assertThat(servicio.consultarEstado().baseDatosDisponible()).isFalse();
    }
}
