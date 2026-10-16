package co.edu.unicesar.sierap.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integración de extremo a extremo del backend: arranca la aplicación
 * completa contra un PostgreSQL real (Testcontainers), con Flyway y JPA activos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SystemStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStatusWithDatabaseAvailable() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("sierap"))
                .andExpect(jsonPath("$.profiles[0]").value("test"))
                .andExpect(jsonPath("$.databaseAvailable").value(true))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unknownPathRespondsWithTheUniformErrorFormat() throws Exception {
        mockMvc.perform(get("/api/path-that-does-not-exist").with(authentication(teacherSession())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"))
                .andExpect(jsonPath("$.path").value("/api/path-that-does-not-exist"));
    }

    @Test
    void unknownPathWithoutSessionResponds401WithoutRevealingTheRoutes() throws Exception {
        mockMvc.perform(get("/api/path-that-does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Debe iniciar sesión para continuar."));
    }

    /** Sesión simulada de un docente, sin pasar por el inicio de sesión. */
    private static UsernamePasswordAuthenticationToken teacherSession() {
        AuthenticatedUser teacher = new AuthenticatedUser(1L, "docente@unicesar.edu.co", UserRole.TEACHER, false);
        return new UsernamePasswordAuthenticationToken(teacher, null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }
}
