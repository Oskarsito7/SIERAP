package co.edu.unicesar.sierap.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.EvaluatorTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas slice web de {@link EvaluatorTypeController}.
 */
@WebMvcTest(controllers = EvaluatorTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({EvaluatorTypeControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class EvaluatorTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvaluatorTypeService evaluatorTypeService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void listCreateUpdateAndDeleteEvaluatorType() throws Exception {
        EvaluatorTypeResponse response = new EvaluatorTypeResponse(
                1L, "Empresario", "Representante de empresa", true, 7);
        when(evaluatorTypeService.listAll()).thenReturn(List.of(response));
        when(evaluatorTypeService.create(any(NewEvaluatorTypeRequest.class))).thenReturn(response);
        when(evaluatorTypeService.update(eq(1L), any(UpdateEvaluatorTypeRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/evaluator-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Empresario"))
                .andExpect(jsonPath("$[0].evaluatorCount").value(7));

        mockMvc.perform(post("/api/evaluator-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NewEvaluatorTypeRequest("Empresario", "Representante de empresa"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(patch("/api/evaluator-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateEvaluatorTypeRequest("Empresario", "Representante de empresa", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/evaluator-types/1"))
                .andExpect(status().isNoContent());

        verify(evaluatorTypeService).delete(1L);
    }
}
