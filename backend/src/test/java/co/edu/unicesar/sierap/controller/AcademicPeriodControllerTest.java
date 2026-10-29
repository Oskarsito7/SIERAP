package co.edu.unicesar.sierap.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.request.UpdateAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.response.AcademicPeriodResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.AcademicPeriodService;
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
 * Pruebas slice web de {@link AcademicPeriodController}.
 */
@WebMvcTest(controllers = AcademicPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AcademicPeriodControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class AcademicPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AcademicPeriodService academicPeriodService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void listCreateUpdateAndDeletePeriod() throws Exception {
        AcademicPeriodResponse response = new AcademicPeriodResponse(
                1L, "2026-2",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 15),
                LocalDate.of(2026, 12, 10),
                "CURRENT");
        when(academicPeriodService.listAll()).thenReturn(List.of(response));
        when(academicPeriodService.create(any(NewAcademicPeriodRequest.class))).thenReturn(response);
        when(academicPeriodService.update(eq(1L), any(UpdateAcademicPeriodRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("2026-2"))
                .andExpect(jsonPath("$[0].status").value("CURRENT"));

        mockMvc.perform(post("/api/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NewAcademicPeriodRequest(
                                        "2026-2",
                                        LocalDate.of(2026, 8, 1),
                                        LocalDate.of(2026, 12, 15),
                                        LocalDate.of(2026, 12, 10)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(patch("/api/periods/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAcademicPeriodRequest(
                                        "2026-2",
                                        LocalDate.of(2026, 8, 1),
                                        LocalDate.of(2026, 12, 15),
                                        LocalDate.of(2026, 12, 10)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2026-2"));

        mockMvc.perform(delete("/api/periods/1"))
                .andExpect(status().isNoContent());

        verify(academicPeriodService).delete(1L);
    }
}
