package co.edu.unicesar.sierap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de {@link JwtService}: emisión, validación y vencimiento de los
 * tokens de acceso, con relojes fijos para simular el paso del tiempo.
 */
class JwtServiceTest {

    private static final String SECRET = "clave-de-pruebas-sierap-solo-para-tests-0123456789";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant NOW = Instant.parse("2026-09-27T14:00:00Z");

    private final Clock clock = Clock.fixed(NOW, BOGOTA);
    private final JwtService jwtService = new JwtService(properties(SECRET), clock);

    @Test
    void issuedTokenCarriesTheUserIdEmailRoleAndPasswordFlag() {
        Teacher teacher = teacher();
        teacher.setMustChangePassword(true);

        AuthenticatedUser user = jwtService.parseAccessToken(jwtService.createAccessToken(teacher)).orElseThrow();

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("marta@unicesar.edu.co");
        assertThat(user.role()).isEqualTo(UserRole.TEACHER);
        assertThat(user.mustChangePassword()).isTrue();
        assertThat(jwtService.getAccessTokenSeconds()).isEqualTo(900);
    }

    @Test
    void tokenIsAlwaysSignedWithHs256WhateverTheKeyLength() {
        JwtService longKey = new JwtService(properties(SECRET.repeat(3)), clock);
        String header = longKey.createAccessToken(teacher()).split("\\.")[0];

        assertThat(new String(Base64.getUrlDecoder().decode(header))).contains("\"alg\":\"HS256\"");
    }

    @Test
    void tokenStopsBeingValidAfterFifteenMinutes() {
        String token = jwtService.createAccessToken(teacher());

        JwtService fourteenMinutesLater = new JwtService(properties(SECRET),
                Clock.fixed(NOW.plus(Duration.ofMinutes(14)), BOGOTA));
        JwtService sixteenMinutesLater = new JwtService(properties(SECRET),
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), BOGOTA));

        assertThat(fourteenMinutesLater.parseAccessToken(token)).isPresent();
        assertThat(sixteenMinutesLater.parseAccessToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyOrAlteredIsRejected() {
        JwtService otherKey = new JwtService(properties("otra-clave-distinta-de-la-de-sierap-0123456789"), clock);
        String foreignToken = otherKey.createAccessToken(teacher());
        String token = jwtService.createAccessToken(teacher());
        String altered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtService.parseAccessToken(foreignToken)).isEmpty();
        assertThat(jwtService.parseAccessToken(altered)).isEmpty();
        assertThat(jwtService.parseAccessToken("no-es-un-jwt")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortOrMissingKey() {
        assertThatThrownBy(() -> new JwtService(properties("corta"), clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtService(properties(""), clock))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Teacher teacher() {
        Teacher teacher = new Teacher("Marta", "Díaz", DocumentType.CC, "1003", "marta@unicesar.edu.co", "hash",
                LocalDateTime.of(2026, 9, 1, 8, 0), new Faculty("04", "Facultad de Ingenierías y Tecnologías"));
        ReflectionTestUtils.setField(teacher, "id", 42L);
        return teacher;
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(secret, 15, 7, false, 72, 1, "http://localhost:5173", "unicesar.edu.co",
                "SIERAP <no-reply@unicesar.edu.co>");
    }
}
