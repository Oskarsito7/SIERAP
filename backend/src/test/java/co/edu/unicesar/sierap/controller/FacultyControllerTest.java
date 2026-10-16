package co.edu.unicesar.sierap.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewFacultyRequest;
import co.edu.unicesar.sierap.dto.request.UpdateFacultyRequest;
import co.edu.unicesar.sierap.dto.response.FacultyResponse;
import co.edu.unicesar.sierap.exception.GlobalExceptionHandler;
import co.edu.unicesar.sierap.service.FacultyService;
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
 * Pruebas slice web de {@link FacultyController}.
 */
@WebMvcTest(controllers = FacultyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({FacultyControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class FacultyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FacultyService facultyService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void listCreateUpdateAndDeleteFaculty() throws Exception {
        FacultyResponse response = new FacultyResponse(1L, "04", "Ingenierías y Tecnológicas", 2);
        when(facultyService.listForUser(any())).thenReturn(List.of(response));
        when(facultyService.create(any(NewFacultyRequest.class))).thenReturn(response);
        when(facultyService.update(eq(1L), any(UpdateFacultyRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/faculties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("04"))
                .andExpect(jsonPath("$[0].programCount").value(2));

        mockMvc.perform(post("/api/faculties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NewFacultyRequest("04", "Ingenierías y Tecnológicas"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(patch("/api/faculties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateFacultyRequest("04", "Ingenierías y Tecnológicas"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ingenierías y Tecnológicas"));

        mockMvc.perform(delete("/api/faculties/1"))
                .andExpect(status().isNoContent());

        verify(facultyService).delete(1L);
    }
}
