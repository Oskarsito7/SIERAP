package co.edu.unicesar.sierap.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifica que {@link GlobalExceptionHandler} traduzca cada tipo de error al
 * código HTTP correcto y al formato uniforme de
 * {@link co.edu.unicesar.sierap.dto.response.ErrorResponse}.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ControladorDePrueba.class)
@Import({GlobalExceptionHandlerTest.ControladorDePrueba.class, GlobalExceptionHandlerTest.RelojDePrueba.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recursoNoEncontradoResponde404ConMensajeYRuta() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("No existe Grupo con id 7"))
                .andExpect(jsonPath("$.ruta").value("/prueba/no-encontrado"))
                .andExpect(jsonPath("$.fechaHora").value("2026-03-15T09:30:00"));
    }

    @Test
    void reglaDeNegocioResponde422() throws Exception {
        mockMvc.perform(get("/prueba/regla-negocio"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("El grupo ya tiene el máximo de integrantes"));
    }

    @Test
    void accesoDenegadoResponde403() throws Exception {
        mockMvc.perform(get("/prueba/acceso-denegado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El grupo no está asignado a este docente"));
    }

    @Test
    void datosInvalidosResponden400ConElErrorDeCadaCampo() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void metodoNoPermitidoResponde405ConElFormatoUniforme() throws Exception {
        mockMvc.perform(post("/prueba/regla-negocio"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensaje").value("Método HTTP no permitido en esta ruta"))
                .andExpect(jsonPath("$.ruta").value("/prueba/regla-negocio"));
    }

    @Test
    void errorInesperadoResponde500SinExponerDetallesInternos() throws Exception {
        mockMvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje", not(containsString("detalle interno"))));
    }

    /** Controller que existe solo en esta prueba para provocar cada tipo de error. */
    @RestController
    @RequestMapping("/prueba")
    static class ControladorDePrueba {

        @GetMapping("/no-encontrado")
        void lanzarRecursoNoEncontrado() {
            throw new RecursoNoEncontradoException("Grupo", 7);
        }

        @GetMapping("/regla-negocio")
        void lanzarReglaNegocio() {
            throw new ReglaNegocioException("El grupo ya tiene el máximo de integrantes");
        }

        @GetMapping("/acceso-denegado")
        void lanzarAccesoDenegado() {
            throw new AccesoDenegadoException("El grupo no está asignado a este docente");
        }

        @PostMapping("/validacion")
        void validar(@Valid @RequestBody SolicitudDePrueba solicitud) {
            // Si la validación pasa no hay nada que hacer.
        }

        @GetMapping("/inesperado")
        void lanzarErrorInesperado() {
            throw new IllegalStateException("detalle interno que no debe llegar al cliente");
        }
    }

    /**
     * DTO de entrada usado solo para probar la validación.
     *
     * @param nombre campo obligatorio
     */
    record SolicitudDePrueba(@NotBlank(message = "El nombre es obligatorio") String nombre) {
    }

    /** Reloj fijo para que {@code fechaHora} sea predecible. */
    @TestConfiguration
    static class RelojDePrueba {

        @Bean
        Clock reloj() {
            return Clock.fixed(Instant.parse("2026-03-15T14:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }
}
