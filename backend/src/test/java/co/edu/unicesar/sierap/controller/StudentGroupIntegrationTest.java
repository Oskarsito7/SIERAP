package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
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
 * Prueba de integración de las operaciones del estudiante sobre los grupos (Etapa 5,
 * {@code docs/07} "Groups"): ver los grupos de su momento, inscribirse, cambiarse,
 * salirse y su historial, contra PostgreSQL real y con un reloj fijo.
 *
 * <p>Hoy es el 28 de septiembre de 2026. Cada prueba crea una facultad con un programa y
 * tres momentos, y se revierte al terminar:</p>
 * <ul>
 *   <li>{@code configuring}: en configuración, semestres 1 a 3, de 3 a 5 integrantes,
 *       conformación abierta (20 al 30 de septiembre).</li>
 *   <li>{@code upcoming}: en configuración, semestres 1 a 3, la conformación abre el 1
 *       de octubre.</li>
 *   <li>{@code launched}: lanzado, semestres 4 a 6.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class StudentGroupIntegrationTest {

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
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Program program;
    private Program otherProgram;
    private Moment configuring;
    private Moment upcoming;
    private Moment launched;
    private Teacher teacher;
    private ProjectGroup first;
    private ProjectGroup second;

    /** Reloj fijo: 28 de septiembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createMomentsAndGroups() {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();

        Faculty faculty = new Faculty("E" + n, "Facultad de estudiantes " + n);
        program = new Program("EP" + n, "Programa de estudiantes " + n);
        otherProgram = new Program("EO" + n, "Otro programa " + n);
        faculty.addProgram(program);
        faculty.addProgram(otherProgram);

        configuring = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 3, 5);
        setWindows(configuring, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        configuring.addLearningOutcome(outcome());
        program.addMoment(configuring);

        upcoming = new Moment(period, 2, "Momento 2", "Asignatura 2", 1, 3, "Bajo", 3, 5);
        setWindows(upcoming, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 31));
        upcoming.addLearningOutcome(outcome());
        program.addMoment(upcoming);

        launched = new Moment(period, 3, "Momento 3", "Asignatura 3", 4, 6, "Medio", 2, 4);
        setWindows(launched, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));
        launched.addLearningOutcome(outcome());
        launched.launch();
        program.addMoment(launched);
        facultyRepository.saveAndFlush(faculty);

        teacher = persist(new Teacher("Docente", "Prueba", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        first = projectGroupRepository.saveAndFlush(new ProjectGroup(configuring, teacher, "Grupo 1"));
        second = projectGroupRepository.saveAndFlush(new ProjectGroup(configuring, teacher, "Grupo 2"));
    }

    // ------------------------------------------------------------ ver los grupos

    @Test
    void theStudentSeesEveryGroupOfTheirMomentButOnlyTheNamesOfOtherMembers() throws Exception {
        Student classmate = student(program, 2);
        first.addMember(classmate);
        first.addEvaluator(externalEvaluator());
        projectGroupRepository.saveAndFlush(first);
        String token = loginToken(student(program, 1));

        mockMvc.perform(get("/api/groups").param("momentId", configuring.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Grupo 1", "Grupo 2")))
                .andExpect(jsonPath("$[0].members[0].fullName").value(classmate.getFullName()))
                .andExpect(jsonPath("$[0].members[0].studentCode").value(nullValue()))
                .andExpect(jsonPath("$[0].members[0].email").value(nullValue()))
                .andExpect(jsonPath("$[0].evaluators", empty()))
                .andExpect(jsonPath("$[0].myGroup").value(false));

        String classmateToken = loginToken(classmate);
        mockMvc.perform(get("/api/groups/" + first.getId()).header(HttpHeaders.AUTHORIZATION,
                        bearer(classmateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myGroup").value(true))
                .andExpect(jsonPath("$.members[0].studentCode").value(classmate.getStudentCode()))
                .andExpect(jsonPath("$.evaluators", empty()));
    }

    @Test
    void studentsOutsideTheMomentCannotSeeItsGroups() throws Exception {
        String otherSemester = loginToken(student(program, 5));
        String otherProgramToken = loginToken(student(otherProgram, 2));

        for (String token : new String[] {otherSemester, otherProgramToken}) {
            mockMvc.perform(get("/api/groups").param("momentId", configuring.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/groups/" + first.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bearer(otherSemester)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------ inscribirse

    @Test
    void theStudentJoinsSwitchesInOneStepAndLeaves() throws Exception {
        String token = loginToken(student(program, 2));

        join(token, first).andExpect(status().isOk())
                .andExpect(jsonPath("$.myGroup").value(true))
                .andExpect(jsonPath("$.memberCount").value(1));

        join(token, second).andExpect(status().isOk())
                .andExpect(jsonPath("$.myGroup").value(true));
        mockMvc.perform(get("/api/groups/" + first.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.memberCount").value(0));

        join(token, second).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Ya es integrante de este grupo."));

        mockMvc.perform(post("/api/groups/" + second.getId() + "/leave")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/groups/" + second.getId() + "/leave")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aFullGroupRejectsNewMembers() throws Exception {
        for (int i = 0; i < configuring.getMaxMembers(); i++) {
            first.addMember(student(program, 1));
        }
        projectGroupRepository.saveAndFlush(first);

        join(loginToken(student(program, 1)), first)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("máximo de 5")));
    }

    @Test
    void joiningAndLeavingOnlyHappenDuringGroupFormation() throws Exception {
        Student member = student(program, 4);
        ProjectGroup launchedGroup = new ProjectGroup(launched, teacher, "Grupo lanzado");
        launchedGroup.addMember(member);
        projectGroupRepository.saveAndFlush(launchedGroup);
        ProjectGroup upcomingGroup = projectGroupRepository.saveAndFlush(
                new ProjectGroup(upcoming, teacher, "Grupo próximo"));

        join(loginToken(student(program, 5)), launchedGroup)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("ya fue lanzado")));
        mockMvc.perform(post("/api/groups/" + launchedGroup.getId() + "/leave")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(member))))
                .andExpect(status().isUnprocessableEntity());
        join(loginToken(student(program, 2)), upcomingGroup)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La conformación de grupos abre el 2026-10-01."));
    }

    @Test
    void onlyStudentsJoin() throws Exception {
        join(loginToken(teacher), first).andExpect(status().isForbidden());
        join(loginToken(student(otherProgram, 2)), first).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ historial

    @Test
    void theHistoryListsOneGroupPerMomentNewestFirstIncludingThoseTheTeacherAddedThemTo() throws Exception {
        Student student = student(program, 2);
        String token = loginToken(student);
        join(token, first).andExpect(status().isOk());
        // Semestre fuera del rango del momento lanzado: lo agregó el docente.
        ProjectGroup launchedGroup = new ProjectGroup(launched, teacher, "Grupo lanzado");
        launchedGroup.addMember(student);
        projectGroupRepository.saveAndFlush(launchedGroup);

        mockMvc.perform(get("/api/students/me/groups").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].momentName", contains("Momento 3", "Momento 1")))
                .andExpect(jsonPath("$[*].myGroup", contains(true, true)));
        mockMvc.perform(get("/api/groups").param("momentId", launched.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Grupo lanzado")));
        mockMvc.perform(get("/api/students/me/groups").header(HttpHeaders.AUTHORIZATION,
                        bearer(loginToken(teacher))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions join(String token, ProjectGroup group) throws Exception {
        return mockMvc.perform(post("/api/groups/" + group.getId() + "/join")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static void setWindows(Moment moment, LocalDate formationStart, LocalDate formationEnd,
                                   LocalDate submissionStart, LocalDate submissionEnd) {
        moment.setGroupFormationStart(formationStart);
        moment.setGroupFormationEnd(formationEnd);
        moment.setSubmissionStart(submissionStart);
        moment.setSubmissionEnd(submissionEnd);
        moment.setGradingStart(LocalDate.of(2026, 11, 2));
        moment.setGradingEnd(LocalDate.of(2026, 11, 20));
    }

    private static LearningOutcome outcome() {
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio", 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, BigDecimal.ZERO, 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal("1.00"), 2));
        LearningOutcome outcome = new LearningOutcome("RAP1", "Descripción");
        outcome.addCriterion(criterion);
        return outcome;
    }

    private Student student(Program ofProgram, int semester) {
        String code = "E" + SEQUENCE.incrementAndGet();
        return persist(new Student("Estudiante", "Prueba " + code, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), code, semester, ofProgram));
    }

    private ExternalEvaluator externalEvaluator() {
        ExternalEvaluator evaluator = new ExternalEvaluator("Evaluador", "Prueba", DocumentType.CC, document(),
                "evaluador.estudiantes" + SEQUENCE.incrementAndGet() + "@correo.com",
                passwordEncoder.encode(PASSWORD), now(),
                evaluatorTypeRepository.findByName("Empresario").orElseThrow());
        evaluator.setCreatedBy(teacher);
        return persist(evaluator);
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
        return "34" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".estudiantes" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
