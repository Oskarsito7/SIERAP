package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la consulta de momentos ({@code GET /api/moments} y
 * {@code GET /api/moments/{id}}) contra PostgreSQL real: alcance por rol y ventanas
 * abiertas calculadas con el {@code Clock}.
 *
 * <p>El reloj se fija en el 28 de septiembre de 2026 (dentro del periodo 2026-2 de la
 * semilla). Cada prueba crea una facultad propia con dos momentos y se revierte al
 * terminar:</p>
 * <ul>
 *   <li>Momento 1 (semestres 1 a 3), en configuración y con la conformación de grupos
 *       abierta.</li>
 *   <li>Momento 2 (semestres 4 a 6), lanzado y con la ventana de entregas abierta.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class MomentIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @TestBean
    private Clock clock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicPeriod period;
    private Faculty faculty;
    private Program program;
    private Moment configuring;
    private Moment launched;

    /** Reloj fijo que usa la aplicación durante estas pruebas. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createFacultyWithTwoMoments() {
        int n = SEQUENCE.incrementAndGet();
        period = academicPeriodRepository.findByCode("2026-2").orElseThrow();
        faculty = new Faculty("T" + n, "Facultad de prueba " + n);
        program = new Program("P" + n, "Programa de prueba " + n);
        faculty.addProgram(program);

        configuring = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 3, 5);
        setWindows(configuring, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30));
        configuring.addLearningOutcome(outcome("RAP1", "0.60"));
        program.addMoment(configuring);

        launched = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        setWindows(launched, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 24));
        launched.setSubmissionStart(LocalDate.of(2026, 9, 1));
        launched.addLearningOutcome(outcome("RAP1", "1.00"));
        launched.launch();
        program.addMoment(launched);

        facultyRepository.saveAndFlush(faculty);
    }

    @Test
    void superAdminSeesTheMomentsOfEveryFaculty() throws Exception {
        String token = loginToken(persist(new SuperAdmin("Super", "Prueba", DocumentType.CC, document(),
                email("super"), passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock))));

        mockMvc.perform(get("/api/moments").param("periodId", period.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(configuring.getId().intValue(), launched.getId().intValue())))
                .andExpect(jsonPath("$[*].programName", hasItem("Ingeniería de Sistemas")));
    }

    @Test
    void teacherSeesTheMomentsOfTheirFacultyWithTheOpenWindows() throws Exception {
        String token = loginToken(persistTeacher(faculty));

        mockMvc.perform(get("/api/moments").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(configuring.getId()))
                .andExpect(jsonPath("$[0].periodCode").value("2026-2"))
                .andExpect(jsonPath("$[0].programName").value(program.getName()))
                .andExpect(jsonPath("$[0].status").value("CONFIGURATION"))
                .andExpect(jsonPath("$[0].groupFormationStart").value("2026-09-20"))
                .andExpect(jsonPath("$[0].groupFormationOpen").value(true))
                .andExpect(jsonPath("$[0].submissionOpen").value(false))
                .andExpect(jsonPath("$[0].gradingOpen").value(false))
                .andExpect(jsonPath("$[1].id").value(launched.getId()))
                .andExpect(jsonPath("$[1].status").value("LAUNCHED"))
                .andExpect(jsonPath("$[1].groupFormationOpen").value(false))
                .andExpect(jsonPath("$[1].submissionOpen").value(true))
                .andExpect(jsonPath("$[1].gradingOpen").value(false));
    }

    @Test
    void detailFlattensTheSummaryAndAddsTheAssessmentStructure() throws Exception {
        String token = loginToken(persistTeacher(faculty));

        mockMvc.perform(get("/api/moments/" + configuring.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configuring.getId()))
                .andExpect(jsonPath("$.name").value("Momento 1"))
                .andExpect(jsonPath("$.semesterFrom").value(1))
                .andExpect(jsonPath("$.groupFormationOpen").value(true))
                .andExpect(jsonPath("$.summary").doesNotExist())
                .andExpect(jsonPath("$.maxScore").value(0.6))
                .andExpect(jsonPath("$.learningOutcomes[0].code").value("RAP1"))
                .andExpect(jsonPath("$.learningOutcomes[0].maxScore").value(0.6))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].description").value("Criterio"))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levels", hasSize(2)))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levels[0].name").value("Bajo"))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levels[1].score").value(0.6));
    }

    @Test
    void adminOfAnotherFacultyCannotSeeTheMoments() throws Exception {
        Faculty engineering = facultyRepository.findByCode("04").orElseThrow();
        String token = loginToken(persist(new Admin("Admin", "Prueba", DocumentType.CC, document(),
                email("admin"), passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), engineering)));

        mockMvc.perform(get("/api/moments").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(configuring.getId().intValue()))))
                .andExpect(jsonPath("$[*].programName", hasItem("Ingeniería de Sistemas")));
        mockMvc.perform(get("/api/moments/" + configuring.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentOnlySeesTheMomentsOfTheirProgramAndSemester() throws Exception {
        String token = loginToken(persist(new Student("Estudiante", "Prueba", DocumentType.TI, document(),
                email("estudiante"), passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock),
                "2026" + SEQUENCE.incrementAndGet(), 2, program)));

        mockMvc.perform(get("/api/moments").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(configuring.getId()));
        mockMvc.perform(get("/api/moments/" + configuring.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/moments/" + launched.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void answersNotFoundForAnUnknownMomentOrPeriod() throws Exception {
        String token = loginToken(persistTeacher(faculty));

        mockMvc.perform(get("/api/moments/0").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/moments").param("periodId", "0").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAnAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/moments")).andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------- utilidades

    /** Ventana de conformación indicada; entregas en octubre y calificación del 2 al 20 de noviembre. */
    private static void setWindows(Moment moment, LocalDate formationStart, LocalDate formationEnd) {
        moment.setGroupFormationStart(formationStart);
        moment.setGroupFormationEnd(formationEnd);
        moment.setSubmissionStart(LocalDate.of(2026, 10, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
        moment.setGradingStart(LocalDate.of(2026, 11, 2));
        moment.setGradingEnd(LocalDate.of(2026, 11, 20));
    }

    private static LearningOutcome outcome(String code, String maxScore) {
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio", 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, BigDecimal.ZERO, 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(maxScore), 2));
        LearningOutcome outcome = new LearningOutcome(code, "Descripción de " + code);
        outcome.addCriterion(criterion);
        return outcome;
    }

    private User persistTeacher(Faculty ofFaculty) {
        return persist(new Teacher("Docente", "Prueba", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), ofFaculty));
    }

    private User persist(User user) {
        return userRepository.saveAndFlush(user);
    }

    private String loginToken(User user) throws Exception {
        ResultActions login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))));
        return JsonPath.read(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.accessToken");
    }

    private static String document() {
        return "50" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".momentos" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
