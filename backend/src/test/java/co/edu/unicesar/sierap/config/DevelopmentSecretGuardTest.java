package co.edu.unicesar.sierap.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Pruebas de {@link DevelopmentSecretGuard}: la clave pública de desarrollo solo se
 * acepta con el perfil {@code dev} activado explícitamente.
 */
class DevelopmentSecretGuardTest {

    private static final String DEV_SECRET = "clave-de-desarrollo-sierap-no-usar-en-produccion-0123456789";

    @Test
    void theDevelopmentSecretFailsWhenDevIsOnlyTheDefaultProfile() {
        MockEnvironment environment = devEnvironment();
        environment.setDefaultProfiles("dev");

        assertThatThrownBy(() -> new DevelopmentSecretGuard(environment, properties(DEV_SECRET)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave pública de desarrollo");
    }

    @Test
    void theDevelopmentSecretIsAcceptedWhenDevIsActivatedExplicitly() {
        MockEnvironment environment = devEnvironment();
        environment.setActiveProfiles("dev");

        assertThatCode(() -> new DevelopmentSecretGuard(environment, properties(DEV_SECRET)))
                .doesNotThrowAnyException();
    }

    @Test
    void anOwnSecretIsAcceptedEvenWithoutAnExplicitProfile() {
        MockEnvironment environment = devEnvironment();
        environment.setDefaultProfiles("dev");

        assertThatCode(() -> new DevelopmentSecretGuard(environment,
                properties("una-clave-propia-de-al-menos-32-bytes-0123")))
                .doesNotThrowAnyException();
    }

    @Test
    void profilesWithoutADevelopmentSecretAreNotAffected() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new DevelopmentSecretGuard(environment,
                properties("una-clave-propia-de-al-menos-32-bytes-0123")))
                .doesNotThrowAnyException();
    }

    private static MockEnvironment devEnvironment() {
        return new MockEnvironment().withProperty(DevelopmentSecretGuard.DEVELOPMENT_SECRET_PROPERTY, DEV_SECRET);
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(secret, 15, 7, false, 72, 1, 5, "http://localhost:5173", "unicesar.edu.co",
                "SIERAP <no-reply@unicesar.edu.co>");
    }
}
