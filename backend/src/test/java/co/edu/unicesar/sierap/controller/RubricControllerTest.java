package co.edu.unicesar.sierap.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewCriterionRequest;
import co.edu.unicesar.sierap.dto.request.NewLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.NewLevelRequest;
import co.edu.unicesar.sierap.dto.request.UpdateCriterionRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLevelRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LearningOutcomeResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.RubricService;
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

/**
 * Pruebas slice web de {@link RubricController}.
 */
@WebMvcTest(controllers = RubricController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RubricControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class RubricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RubricService rubricService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }

    @Test
    void createLearningOutcomeReturns201() throws Exception {
        LearningOutcomeResponse response = new LearningOutcomeResponse(
                300L, "RAP1", "Resultado 1", BigDecimal.ZERO, List.of());

        when(rubricService.createLearningOutcome(any(), eq(10L), any(NewLearningOutcomeRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "code": "RAP1",
                  "description": "Resultado 1"
                }
                """;

        mockMvc.perform(post("/api/moments/10/learning-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(300))
                .andExpect(jsonPath("$.code").value("RAP1"));
    }

    @Test
    void updateLearningOutcomeReturns200() throws Exception {
        LearningOutcomeResponse response = new LearningOutcomeResponse(
                300L, "RAP2", "Resultado Modificado", BigDecimal.ZERO, List.of());

        when(rubricService.updateLearningOutcome(any(), eq(300L), any(UpdateLearningOutcomeRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "code": "RAP2",
                  "description": "Resultado Modificado"
                }
                """;

        mockMvc.perform(patch("/api/learning-outcomes/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RAP2"));
    }

    @Test
    void deleteLearningOutcomeReturns204() throws Exception {
        mockMvc.perform(delete("/api/learning-outcomes/300"))
                .andExpect(status().isNoContent());

        verify(rubricService).deleteLearningOutcome(any(), eq(300L));
    }

    @Test
    void createCriterionReturns201() throws Exception {
        CriterionResponse response = new CriterionResponse(
                400L, "Identificación del problema", 1, BigDecimal.ZERO, List.of());

        when(rubricService.createCriterion(any(), eq(300L), any(NewCriterionRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "description": "Identificación del problema",
                  "displayOrder": 1
                }
                """;

        mockMvc.perform(post("/api/learning-outcomes/300/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(400))
                .andExpect(jsonPath("$.description").value("Identificación del problema"))
                .andExpect(jsonPath("$.displayOrder").value(1));
    }

    @Test
    void updateCriterionReturns200() throws Exception {
        CriterionResponse response = new CriterionResponse(
                400L, "Nueva descripción", 2, BigDecimal.ZERO, List.of());

        when(rubricService.updateCriterion(any(), eq(400L), any(UpdateCriterionRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "description": "Nueva descripción",
                  "displayOrder": 2
                }
                """;

        mockMvc.perform(patch("/api/criteria/400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nueva descripción"))
                .andExpect(jsonPath("$.displayOrder").value(2));
    }

    @Test
    void deleteCriterionReturns204() throws Exception {
        mockMvc.perform(delete("/api/criteria/400"))
                .andExpect(status().isNoContent());

        verify(rubricService).deleteCriterion(any(), eq(400L));
    }

    @Test
    void createLevelReturns201() throws Exception {
        LevelResponse response = new LevelResponse(
                500L, "Alto", "Desempeño esperado", BigDecimal.valueOf(0.48), 2);

        when(rubricService.createLevel(any(), eq(400L), any(NewLevelRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "name": "Alto",
                  "description": "Desempeño esperado",
                  "score": 0.48,
                  "displayOrder": 2
                }
                """;

        mockMvc.perform(post("/api/criteria/400/levels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.name").value("Alto"))
                .andExpect(jsonPath("$.score").value(0.48))
                .andExpect(jsonPath("$.displayOrder").value(2));
    }

    @Test
    void updateLevelReturns200() throws Exception {
        LevelResponse response = new LevelResponse(
                500L, "Sobresaliente", "Excelente", BigDecimal.valueOf(0.60), 3);

        when(rubricService.updateLevel(any(), eq(500L), any(UpdateLevelRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "name": "Sobresaliente",
                  "description": "Excelente",
                  "score": 0.60,
                  "displayOrder": 3
                }
                """;

        mockMvc.perform(patch("/api/levels/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sobresaliente"))
                .andExpect(jsonPath("$.score").value(0.60));
    }

    @Test
    void deleteLevelReturns204() throws Exception {
        mockMvc.perform(delete("/api/levels/500"))
                .andExpect(status().isNoContent());

        verify(rubricService).deleteLevel(any(), eq(500L));
    }
}
