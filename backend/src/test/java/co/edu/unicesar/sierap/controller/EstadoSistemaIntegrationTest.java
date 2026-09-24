package co.edu.unicesar.sierap.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
class EstadoSistemaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respondeEstadoConBaseDeDatosDisponible() throws Exception {
        mockMvc.perform(get("/api/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacion").value("sierap"))
                .andExpect(jsonPath("$.perfiles[0]").value("test"))
                .andExpect(jsonPath("$.baseDatosDisponible").value(true))
                .andExpect(jsonPath("$.fechaHora").isNotEmpty());
    }

    @Test
    void rutaInexistenteRespondeConElFormatoDeErrorUniforme() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("La ruta solicitada no existe"))
                .andExpect(jsonPath("$.ruta").value("/api/ruta-que-no-existe"));
    }
}
