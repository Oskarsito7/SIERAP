package co.edu.unicesar.sierap.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewProgramRequest;
import co.edu.unicesar.sierap.dto.request.UpdateProgramRequest;
import co.edu.unicesar.sierap.dto.response.ProgramResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.ProgramService;
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
 * Pruebas slice web de {@link ProgramController}.
 */
@WebMvcTest(controllers = ProgramController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ProgramControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class ProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProgramService programService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void listCreateUpdateAndDeleteProgram() throws Exception {
        ProgramResponse response = new ProgramResponse(
                10L, "108", "Ingeniería de Sistemas", 1L, "Ingenierías y Tecnológicas", 2, 48);
        when(programService.listForUser(any(), eq(1L))).thenReturn(List.of(response));
        when(programService.create(any(NewProgramRequest.class))).thenReturn(response);
        when(programService.update(eq(10L), any(UpdateProgramRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/programs").param("facultyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].code").value("108"))
                .andExpect(jsonPath("$[0].studentCount").value(48));

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NewProgramRequest(1L, "108", "Ingeniería de Sistemas"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(patch("/api/programs/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProgramRequest("108", "Ingeniería de Sistemas"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ingeniería de Sistemas"));

        mockMvc.perform(delete("/api/programs/10"))
                .andExpect(status().isNoContent());

        verify(programService).delete(10L);
    }
}
