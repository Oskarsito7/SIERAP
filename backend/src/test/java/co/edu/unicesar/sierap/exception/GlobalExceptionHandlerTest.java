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
import org.springframework.dao.DataIntegrityViolationException;
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
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandlerTest.TestClock.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resourceNotFoundResponds404WithMessageAndPath() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No existe Grupo con id 7"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").value("2026-03-15T09:30:00"));
    }

    @Test
    void businessRuleResponds422() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El grupo ya tiene el máximo de integrantes"));
    }

    @Test
    void forbiddenAccessResponds403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("El grupo no está asignado a este docente"));
    }

    @Test
    void invalidDataResponds400WithTheErrorOfEachField() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.fieldErrors.name").value("El nombre es obligatorio"));
    }

    @Test
    void methodNotAllowedResponds405WithTheUniformFormat() throws Exception {
        mockMvc.perform(post("/test/business-rule"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Método HTTP no permitido en esta ruta"))
                .andExpect(jsonPath("$.path").value("/test/business-rule"));
    }

    @Test
    void dataIntegrityViolationResponds409WithoutExposingTheSqlError() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("entra en conflicto")))
                .andExpect(jsonPath("$.message", not(containsString("fk_teacher_faculty"))));
    }

    @Test
    void unexpectedErrorResponds500WithoutExposingInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("internal detail"))));
    }

    /** Controller que existe solo en esta prueba para provocar cada tipo de error. */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        void throwResourceNotFound() {
            throw new ResourceNotFoundException("Grupo", 7);
        }

        @GetMapping("/business-rule")
        void throwBusinessRule() {
            throw new BusinessRuleException("El grupo ya tiene el máximo de integrantes");
        }

        @GetMapping("/forbidden")
        void throwForbiddenAccess() {
            throw new ForbiddenAccessException("El grupo no está asignado a este docente");
        }

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
            // Si la validación pasa no hay nada que hacer.
        }

        @GetMapping("/data-integrity")
        void throwDataIntegrityViolation() {
            throw new DataIntegrityViolationException(
                    "update or delete on table \"faculty\" violates foreign key constraint \"fk_teacher_faculty\"");
        }

        @GetMapping("/unexpected")
        void throwUnexpectedError() {
            throw new IllegalStateException("internal detail that must not reach the client");
        }
    }

    /**
     * DTO de entrada usado solo para probar la validación.
     *
     * @param name campo obligatorio
     */
    record TestRequest(@NotBlank(message = "El nombre es obligatorio") String name) {
    }

    /** Reloj fijo para que {@code timestamp} sea predecible. */
    @TestConfiguration
    static class TestClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-03-15T14:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }
}
