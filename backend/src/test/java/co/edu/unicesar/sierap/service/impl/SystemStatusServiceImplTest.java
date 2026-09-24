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

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests of {@link SystemStatusServiceImpl}, with a mocked database (Mockito)
 * and a fixed clock.
 */
@ExtendWith(MockitoExtension.class)
class SystemStatusServiceImplTest {

    /** 14:30 UTC is 09:30 in Bogotá (UTC-5). */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-15T14:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private MockEnvironment environment;

    private SystemStatusServiceImpl service;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        service = new SystemStatusServiceImpl(dataSource, FIXED_CLOCK, environment, "sierap", "0.1.0");
    }

    @Test
    void reportsDatabaseAvailableWhenTheConnectionIsValid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        SystemStatusResponse status = service.getStatus();

        assertThat(status.databaseAvailable()).isTrue();
        assertThat(status.application()).isEqualTo("sierap");
        assertThat(status.version()).isEqualTo("0.1.0");
        assertThat(status.profiles()).containsExactly("prod");
        assertThat(status.timestamp()).isEqualTo(LocalDateTime.of(2026, 3, 15, 9, 30));
        verify(connection).close();
    }

    @Test
    void reportsTheDefaultProfilesWhenNoProfileIsActive() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        environment.setActiveProfiles();
        environment.setDefaultProfiles("dev");

        assertThat(service.getStatus().profiles()).containsExactly("dev");
    }

    @Test
    void reportsDatabaseUnavailableWhenTheConnectionFails() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        assertThat(service.getStatus().databaseAvailable()).isFalse();
    }
}
