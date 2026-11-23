package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.RegistrationCode;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.RegistrationCodeRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.service.EmailService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la paginación opcional en el servidor (ADR-024) de
 * {@code GET /api/students}, {@code GET /api/registration-codes} y
 * {@code GET /api/external-evaluators}, contra PostgreSQL real.
 *
 * <p>Cada prueba crea una facultad con un programa, un docente y cinco estudiantes,
 * tres códigos y tres evaluadores externos; se revierte al terminar. Verifica el tamaño
 * de la página, el orden, {@code X-Total-Count}, el buscador {@code q} y que sin
 * {@code page} ni {@code size} la respuesta sea la lista completa de siempre.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PaginationIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final String TOTAL = "X-Total-Count";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private RegistrationCodeRepository registrationCodeRepository;

    @Autowired
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    private Program program;
    private Teacher teacher;
    private String token;

    @BeforeEach
    void createFacultyTeacherAndData() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Faculty faculty = new Faculty("P" + n, "Facultad de paginación " + n);
        program = new Program("PP" + n, "Programa de paginación " + n);
        faculty.addProgram(program);
        facultyRepository.saveAndFlush(faculty);
        teacher = persist(new Teacher("Docente", "Paginación " + n, DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));

        // Solo "Pérez" lleva tilde en un apellido y ningún otro apellido comparte su inicial,
        // para que el orden esperado no dependa de la colación de la base.
        student("Luis", "Alvarez", "A100");
        student("María", "Bravo", "B200");
        student("José", "Pérez", "C300");
        student("Ana", "Quintero", "D400");
        student("Pedro", "Zapata", "E500");

        for (int i = 0; i < 3; i++) {
            registrationCodeRepository.saveAndFlush(new RegistrationCode("PAG" + n + i, teacher, program,
                    LocalDate.now(clock).plusDays(10), now().plusMinutes(i)));
            evaluator("Evaluador" + i, "Apellido" + (char) ('A' + i));
        }
        token = loginToken(teacher);
    }

    // ------------------------------------------------------------ estudiantes

    @Test
    void withoutPageOrSizeTheStudentListIsTheCompleteListWithoutTotalHeader() throws Exception {
        students(Map.of())
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(TOTAL))
                .andExpect(jsonPath("$[*].lastNames", contains("Alvarez", "Bravo", "Pérez", "Quintero", "Zapata")));
    }

    @Test
    void studentsArePagedInTheQueryKeepingTheOrderAndReportingTheTotal() throws Exception {
        students(Map.of("page", 0, "size", 2))
                .andExpect(status().isOk())
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$[*].lastNames", contains("Alvarez", "Bravo")));
        students(Map.of("page", 1, "size", 2))
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$[*].lastNames", contains("Pérez", "Quintero")));
        students(Map.of("page", 2, "size", 2))
                .andExpect(jsonPath("$[*].lastNames", contains("Zapata")));
        students(Map.of("page", 3, "size", 2))
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void onlyOneOfPageOrSizeIsEnoughToPaginate() throws Exception {
        students(Map.of("size", 3))
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$", hasSize(3)));
        students(Map.of("page", 0))
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void theSearchIgnoresCaseAndAccentsInNamesLastNamesCodeAndEmail() throws Exception {
        students(Map.of("q", "perez"))
                .andExpect(jsonPath("$[*].lastNames", contains("Pérez")));
        students(Map.of("q", "PÉREZ"))
                .andExpect(jsonPath("$[*].lastNames", contains("Pérez")));
        students(Map.of("q", "jose"))
                .andExpect(jsonPath("$[*].firstNames", contains("José")));
        students(Map.of("q", "ÁLVAREZ"))
                .andExpect(jsonPath("$[*].lastNames", contains("Alvarez")));
        students(Map.of("q", "maria"))
                .andExpect(jsonPath("$[*].lastNames", contains("Bravo")));
        students(Map.of("q", "pe"))
                .andExpect(jsonPath("$[*].lastNames", contains("Pérez", "Zapata")));
        students(Map.of("q", "c300"))
                .andExpect(jsonPath("$[*].studentCode", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("C300"))))
                .andExpect(jsonPath("$", hasSize(1)));
        // Solo el correo contiene este texto: prueba que también se busca por correo.
        students(Map.of("q", "@UNICESAR.edu.co"))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void theSearchCombinesWithThePaginationAndReportsTheFilteredTotal() throws Exception {
        students(Map.of("q", "@unicesar", "page", 0, "size", 2))
                .andExpect(header().string(TOTAL, "5"))
                .andExpect(jsonPath("$[*].lastNames", contains("Alvarez", "Bravo")));
        students(Map.of("q", "pe", "page", 0, "size", 1))
                .andExpect(header().string(TOTAL, "2"))
                .andExpect(jsonPath("$[*].lastNames", contains("Pérez")));
        students(Map.of("q", "pe", "page", 1, "size", 1))
                .andExpect(header().string(TOTAL, "2"))
                .andExpect(jsonPath("$[*].lastNames", contains("Zapata")));
        students(Map.of("q", "no-existe", "page", 0, "size", 10))
                .andExpect(header().string(TOTAL, "0"))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void searchWildcardsAreTakenLiterally() throws Exception {
        students(Map.of("q", "%"))
                .andExpect(jsonPath("$", empty()));
        students(Map.of("q", "_"))
                .andExpect(jsonPath("$", empty()));
        students(Map.of("q", "   "))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void invalidPageOrSizeAreRejected() throws Exception {
        students(Map.of("page", -1, "size", 10)).andExpect(status().isBadRequest());
        students(Map.of("page", 0, "size", 0)).andExpect(status().isBadRequest());
        students(Map.of("page", 0, "size", 201)).andExpect(status().isBadRequest());
        students(Map.of("page", 0, "size", 200)).andExpect(status().isOk());
    }

    // ----------------------------------------------------- códigos de registro

    @Test
    void registrationCodesAreListedNewestFirstAndPagedOnRequest() throws Exception {
        mockMvc.perform(get("/api/registration-codes").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(TOTAL))
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/registration-codes").param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(TOTAL, "3"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value(org.hamcrest.Matchers.endsWith("2")))
                .andExpect(jsonPath("$[1].code").value(org.hamcrest.Matchers.endsWith("1")));
        mockMvc.perform(get("/api/registration-codes").param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(header().string(TOTAL, "3"))
                .andExpect(jsonPath("$[*].code", contains(org.hamcrest.Matchers.endsWith("0"))));
        mockMvc.perform(get("/api/registration-codes").param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------- evaluadores externos

    @Test
    void externalEvaluatorsAreListedByLastNameAndPagedOnRequest() throws Exception {
        mockMvc.perform(get("/api/external-evaluators").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(TOTAL))
                .andExpect(jsonPath("$[*].lastNames", contains("ApellidoA", "ApellidoB", "ApellidoC")));

        mockMvc.perform(get("/api/external-evaluators").param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(TOTAL, "3"))
                .andExpect(jsonPath("$[*].lastNames", contains("ApellidoA", "ApellidoB")));
        mockMvc.perform(get("/api/external-evaluators").param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(header().string(TOTAL, "3"))
                .andExpect(jsonPath("$[*].lastNames", contains("ApellidoC")));
        mockMvc.perform(get("/api/external-evaluators").param("page", "-1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions students(Map<String, Object> params) throws Exception {
        var request = get("/api/students")
                .param("programId", program.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        params.forEach((name, value) -> request.param(name, value.toString()));
        return mockMvc.perform(request);
    }

    private Student student(String firstNames, String lastNames, String studentCode) {
        int n = SEQUENCE.incrementAndGet();
        Student student = new Student(firstNames, lastNames, DocumentType.TI, document(),
                lastNames.toLowerCase().replace("á", "a").replace("é", "e").replace("ñ", "n") + n
                        + "@unicesar.edu.co",
                passwordEncoder.encode(PASSWORD), now(), studentCode + "-" + n, 2, program);
        student.setCreatedBy(teacher);
        return persist(student);
    }

    private ExternalEvaluator evaluator(String firstNames, String lastNames) {
        ExternalEvaluator created = new ExternalEvaluator(firstNames, lastNames, DocumentType.CC, document(),
                email("evaluador"), passwordEncoder.encode(PASSWORD), now(),
                evaluatorTypeRepository.findByName("Empresario").orElseThrow());
        created.setCreatedBy(teacher);
        return persist(created);
    }

    private <T extends User> T persist(T user) {
        return userRepository.saveAndFlush(user);
    }

    private String loginToken(User user) throws Exception {
        ResultActions login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))));
        return JsonPath.read(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.accessToken");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String document() {
        return "55" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".paginacion" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
