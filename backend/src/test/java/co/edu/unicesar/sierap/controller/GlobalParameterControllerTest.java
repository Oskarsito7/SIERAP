package co.edu.unicesar.sierap.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.SetGlobalParameterRequest;
import co.edu.unicesar.sierap.dto.response.GlobalParameterResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.GlobalParameterService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas slice web de {@link GlobalParameterController}.
 */
@WebMvcTest(controllers = GlobalParameterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalParameterControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class GlobalParameterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GlobalParameterService globalParameterService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void listAllAndSetGlobalParameter() throws Exception {
        GlobalParameterResponse response = new GlobalParameterResponse(
                "min_passing_grade", "3.00", "Nota mínima de aprobación institucional");
        when(globalParameterService.listAll()).thenReturn(List.of(response));
        when(globalParameterService.set(eq("min_passing_grade"), any(SetGlobalParameterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/global-parameters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("min_passing_grade"))
                .andExpect(jsonPath("$[0].value").value("3.00"));

        mockMvc.perform(put("/api/global-parameters/min_passing_grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SetGlobalParameterRequest("3.00", "Nota mínima de aprobación institucional"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("min_passing_grade"))
                .andExpect(jsonPath("$.value").value("3.00"));
    }
}
