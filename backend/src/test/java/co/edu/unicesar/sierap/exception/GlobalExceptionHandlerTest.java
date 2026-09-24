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
 * Checks that {@link GlobalExceptionHandler} translates each kind of error into
 * the right HTTP status and the uniform
 * {@link co.edu.unicesar.sierap.dto.response.ErrorResponse} format.
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
    void unexpectedErrorResponds500WithoutExposingInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("internal detail"))));
    }

    /** Controller that exists only in this test, to trigger each kind of error. */
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
            // Nothing to do when validation passes.
        }

        @GetMapping("/unexpected")
        void throwUnexpectedError() {
            throw new IllegalStateException("internal detail that must not reach the client");
        }
    }

    /**
     * Input DTO used only to test validation.
     *
     * @param name required field
     */
    record TestRequest(@NotBlank(message = "El nombre es obligatorio") String name) {
    }

    /** Fixed clock so {@code timestamp} is predictable. */
    @TestConfiguration
    static class TestClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-03-15T14:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }
}
