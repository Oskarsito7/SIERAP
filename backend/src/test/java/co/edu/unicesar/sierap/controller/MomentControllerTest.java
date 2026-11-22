package co.edu.unicesar.sierap.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.CopyMomentsRequest;
import co.edu.unicesar.sierap.dto.request.LaunchMomentRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse.EvaluatorTypeWithoutWeightResponse;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse.GroupBelowMinimumResponse;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse.TeacherSummary;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse.EvaluatorWeightItemResponse;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.MomentService;
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

/**
 * Pruebas slice web de {@link MomentController} para verificar rutas HTTP,
 * serialización/deserialización y códigos de estado.
 */
@WebMvcTest(controllers = MomentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MomentControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class MomentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MomentService momentService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }

    private MomentDetailResponse createSampleDetail(Long id) {
        MomentSummaryResponse summary = new MomentSummaryResponse(
                id, 1, "Momento 1", "Asignatura", 100L, "Sistemas", 1L, "2026-2",
                1, 3, "Bajo", 3, 5, 0, MomentStatus.CONFIGURATION,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20),
                null, null, null, null,
                true, false, false);
        return new MomentDetailResponse(summary, BigDecimal.valueOf(100), List.of(),
                BigDecimal.valueOf(100), BigDecimal.ZERO, List.of());
    }

    @Test
    void listMomentsReturns200() throws Exception {
        when(momentService.list(any(), eq(1L))).thenReturn(List.of(createSampleDetail(10L).summary()));

        mockMvc.perform(get("/api/moments").param("periodId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Momento 1"));
    }

    @Test
    void getMomentReturns200() throws Exception {
        when(momentService.get(any(), eq(10L))).thenReturn(createSampleDetail(10L));

        mockMvc.perform(get("/api/moments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.teacherWeight").value(100));
    }

    @Test
    void createMomentReturns201() throws Exception {
        when(momentService.create(any(), any(NewMomentRequest.class))).thenReturn(createSampleDetail(10L));

        String body = """
                {
                  "programId": 100,
                  "periodId": 1,
                  "number": 1,
                  "name": "Momento 1",
                  "subject": "Programación",
                  "semesterFrom": 1,
                  "semesterTo": 3,
                  "requirementLevel": "Bajo",
                  "minMembers": 3,
                  "maxMembers": 5,
                  "groupFormationStart": "2026-08-10",
                  "groupFormationEnd": "2026-08-20"
                }
                """;

        mockMvc.perform(post("/api/moments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Momento 1"));
    }

    @Test
    void createMomentReturns400WhenRequiredFieldMissing() throws Exception {
        String body = """
                {
                  "programId": 100,
                  "periodId": 1,
                  "number": 1
                }
                """;

        mockMvc.perform(post("/api/moments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateMomentReturns200() throws Exception {
        when(momentService.update(any(), eq(10L), any(UpdateMomentRequest.class)))
                .thenReturn(createSampleDetail(10L));

        String body = """
                {
                  "name": "Momento Editado",
                  "groupFormationStart": null,
                  "groupFormationEnd": null
                }
                """;

        mockMvc.perform(patch("/api/moments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        verify(momentService).update(any(), eq(10L), any(UpdateMomentRequest.class));
    }

    @Test
    void deleteMomentReturns204() throws Exception {
        mockMvc.perform(delete("/api/moments/10"))
                .andExpect(status().isNoContent());

        verify(momentService).delete(any(), eq(10L));
    }

    @Test
    void updateWeightsReturns200() throws Exception {
        MomentWeightsResponse response = new MomentWeightsResponse(
                BigDecimal.valueOf(60),
                List.of(new EvaluatorWeightItemResponse(1L, "Empresario", BigDecimal.valueOf(40))),
                BigDecimal.valueOf(100));

        when(momentService.updateWeights(any(), eq(10L), any(MomentWeightsRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "teacherWeight": 60,
                  "evaluatorWeights": [
                    { "evaluatorTypeId": 1, "weight": 40 }
                  ]
                }
                """;

        mockMvc.perform(put("/api/moments/10/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherWeight").value(60))
                .andExpect(jsonPath("$.totalWeight").value(100))
                .andExpect(jsonPath("$.evaluatorWeights[0].evaluatorTypeId").value(1))
                .andExpect(jsonPath("$.evaluatorWeights[0].evaluatorTypeName").value("Empresario"));
    }

    @Test
    void launchCheckReturns200() throws Exception {
        LaunchCheckResponse response = new LaunchCheckResponse(
                10L,
                true,
                List.of(),
                3,
                List.of(new GroupBelowMinimumResponse(1L, "Grupo 1", new TeacherSummary(12L, "Marta Díaz"), 2)),
                List.of(new EvaluatorTypeWithoutWeightResponse(2L, "Egresado", 1)));

        when(momentService.launchCheck(any(), eq(10L))).thenReturn(response);

        mockMvc.perform(get("/api/moments/10/launch-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.momentId").value(10))
                .andExpect(jsonPath("$.canLaunch").value(true))
                .andExpect(jsonPath("$.minMembers").value(3))
                .andExpect(jsonPath("$.groupsBelowMinimum[0].name").value("Grupo 1"))
                .andExpect(jsonPath("$.groupsBelowMinimum[0].teacher.fullName").value("Marta Díaz"))
                .andExpect(jsonPath("$.evaluatorTypesWithoutWeight[0].evaluatorTypeName").value("Egresado"));
    }

    @Test
    void launchMomentReturns200() throws Exception {
        MomentSummaryResponse summary = new MomentSummaryResponse(
                10L, 1, "Momento 1", "Asignatura", 100L, "Sistemas", 1L, "2026-2",
                1, 3, "Bajo", 3, 5, 0, MomentStatus.LAUNCHED,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20),
                false, false, false);
        MomentDetailResponse detail = new MomentDetailResponse(summary, BigDecimal.valueOf(100), List.of(),
                BigDecimal.valueOf(100), BigDecimal.ZERO, List.of());

        when(momentService.launch(any(), eq(10L), any(LaunchMomentRequest.class))).thenReturn(detail);

        String body = """
                {
                  "confirmUnderMinimum": true
                }
                """;

        mockMvc.perform(post("/api/moments/10/launch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("LAUNCHED"));
    }

    @Test
    void launchMomentWithoutBodyReturns200() throws Exception {
        MomentSummaryResponse summary = new MomentSummaryResponse(
                10L, 1, "Momento 1", "Asignatura", 100L, "Sistemas", 1L, "2026-2",
                1, 3, "Bajo", 3, 5, 0, MomentStatus.LAUNCHED,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20),
                false, false, false);
        MomentDetailResponse detail = new MomentDetailResponse(summary, BigDecimal.valueOf(100), List.of(),
                BigDecimal.valueOf(100), BigDecimal.ZERO, List.of());

        when(momentService.launch(any(), eq(10L), any())).thenReturn(detail);

        mockMvc.perform(post("/api/moments/10/launch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("LAUNCHED"));
    }

    @Test
    void copyMomentsReturns201() throws Exception {
        MomentSummaryResponse summary = createSampleDetail(20L).summary();
        when(momentService.copy(any(), any(CopyMomentsRequest.class))).thenReturn(List.of(summary));

        String body = """
                {
                  "fromPeriodId": 1,
                  "toPeriodId": 2,
                  "programId": 100
                }
                """;

        mockMvc.perform(post("/api/moments/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].name").value("Momento 1"));
    }

    @Test
    void copyMomentsReturns400WhenRequiredFieldMissing() throws Exception {
        String body = """
                {
                  "fromPeriodId": 1
                }
                """;

        mockMvc.perform(post("/api/moments/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
