package co.edu.unicesar.sierap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import co.edu.unicesar.sierap.entity.AuditLog;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.AuditLogRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicesar.sierap.service.EmailService;

/**
 * Prueba de integración de la bitácora de auditoría y de la prórroga excepcional de un
 * momento lanzado (ADR-023), contra PostgreSQL real. El reloj se fija en el 28 de
 * septiembre de 2026 y el periodo 2026-2 de la semilla tiene el límite de la UPC el 28
 * de noviembre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuditLogIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String REASON = "Paro estudiantil de dos semanas";

    @TestBean
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Faculty faculty;
    private Program program;
    private Moment launched;
    private Moment configuring;
    private Admin admin;
    private Admin otherAdmin;
    private SuperAdmin superAdmin;

    /** Reloj fijo que usa la aplicación durante estas pruebas. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createScenario() {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();
        faculty = new Faculty("AU" + n, "Facultad de auditoría " + n);
        program = new Program("PA" + n, "Programa de auditoría " + n);
        faculty.addProgram(program);

        launched = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 3, 5);
        windows(launched);
        launched.addLearningOutcome(outcome());
        launched.launch();
        program.addMoment(launched);

        configuring = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 3, 5);
        windows(configuring);
        configuring.addLearningOutcome(outcome());
        program.addMoment(configuring);
        facultyRepository.saveAndFlush(faculty);

        Faculty other = new Faculty("AO" + n, "Otra facultad " + n);
        facultyRepository.saveAndFlush(other);

        admin = persist(new Admin("Admin", "Auditoría", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), faculty));
        otherAdmin = persist(new Admin("Admin", "Ajeno", DocumentType.CC, document(), email("ajeno"),
                passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), other));
        superAdmin = persist(new SuperAdmin("Super", "Auditoría", DocumentType.CC, document(), email("super"),
                passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock)));
    }

    // ------------------------------------------------------------ prórroga

    @Test
    void aValidExtensionUpdatesTheDatesAndLeavesAnAuditRecord() throws Exception {
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-25", "reason", REASON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gradingEnd").value("2026-11-25"))
                .andExpect(jsonPath("$.submissionEnd").value("2026-10-31"))
                .andExpect(jsonPath("$.status").value("LAUNCHED"));

        AuditLog log = onlyEvent(AuditAction.MOMENT_EXTENDED, launched.getId());
        assertThat(log.getActorId()).isEqualTo(admin.getId());
        assertThat(log.getActorName()).isEqualTo("Admin Auditoría");
        assertThat(log.getFacultyId()).isEqualTo(faculty.getId());
        assertThat(log.getSummary()).contains("fin de calificación 2026-11-20 a 2026-11-25").contains(REASON);
        assertThat(log.getIp()).isNotBlank();
    }

    @Test
    void shorteningADateIs422() throws Exception {
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-10", "reason", REASON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("solo se pueden ampliar")));
    }

    @Test
    void passingTheUpcDeadlineIs422() throws Exception {
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-29", "reason", REASON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("límite de evaluación de la UPC")));
    }

    @Test
    void aMomentThatIsNotLaunchedIs422() throws Exception {
        extend(loginToken(admin), configuring.getId(), Map.of("gradingEnd", "2026-11-25", "reason", REASON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Solo se puede prorrogar un momento ya lanzado."));
    }

    @Test
    void anAdminOfAnotherFacultyIsForbiddenAndAReasonIsRequired() throws Exception {
        extend(loginToken(otherAdmin), launched.getId(), Map.of("gradingEnd", "2026-11-25", "reason", REASON))
                .andExpect(status().isForbidden());
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-25", "reason", "corto"))
                .andExpect(status().isBadRequest());
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-25"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ eventos

    @Test
    void launchingAMomentLeavesAnAuditRecord() throws Exception {
        mockMvc.perform(post("/api/moments/" + configuring.getId() + "/launch")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LAUNCHED"));

        AuditLog log = onlyEvent(AuditAction.MOMENT_LAUNCHED, configuring.getId());
        assertThat(log.getSummary()).startsWith("Lanzó el Momento 2");
        assertThat(log.getFacultyId()).isEqualTo(faculty.getId());
    }

    @Test
    void editingAnEmailLeavesAnAuditRecordWithTheOldAndNewValues() throws Exception {
        Student student = persist(new Student("Ana", "Pérez", DocumentType.TI, document(), email("ana"),
                passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), "AU" + SEQUENCE.incrementAndGet(), 2,
                program));
        String oldEmail = student.getEmail();
        String newEmail = "nuevo.ana" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";

        mockMvc.perform(patch("/api/users/" + student.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", newEmail))))
                .andExpect(status().isOk());

        AuditLog log = onlyEvent(AuditAction.ACCOUNT_UPDATED, student.getId());
        assertThat(log.getSummary()).isEqualTo("Cambió el correo (de " + oldEmail + " a " + newEmail
                + ") de la cuenta de Ana Pérez.");
        assertThat(log.getFacultyId()).isEqualTo(faculty.getId());
    }

    @Test
    void aFailedOperationLeavesNoRecord() throws Exception {
        long before = auditLogRepository.count();
        extend(loginToken(admin), launched.getId(), Map.of("gradingEnd", "2026-11-10", "reason", REASON))
                .andExpect(status().isUnprocessableEntity());
        assertThat(auditLogRepository.count()).isEqualTo(before);
    }

    // ------------------------------------------------------------ consulta

    @Test
    void anAdminOnlySeesTheEventsOfTheirFacultyAndTheSuperAdminSeesAll() throws Exception {
        String token = "alcance" + SEQUENCE.incrementAndGet();
        LocalDateTime when = LocalDateTime.now(clock);
        auditLogRepository.save(new AuditLog(when, null, "Alguien", AuditAction.ACCOUNT_UPDATED, "User", 1L,
                faculty.getId(), "Evento propio " + token, null));
        auditLogRepository.save(new AuditLog(when.plusMinutes(1), null, "Alguien", AuditAction.ACCOUNT_UPDATED,
                "User", 2L, faculty.getId() + 9000, "Evento ajeno " + token, null));
        auditLogRepository.save(new AuditLog(when.plusMinutes(2), null, "Alguien", AuditAction.GLOBAL_PARAMETER_CHANGED,
                "GlobalParameter", 3L, null, "Evento global " + token.toUpperCase(), null));

        mockMvc.perform(get("/api/audit-log").param("q", token)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].summary").value("Evento propio " + token))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
        mockMvc.perform(get("/api/audit-log").param("q", token).param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].summary").value("Evento global " + token.toUpperCase()))
                .andExpect(jsonPath("$.size").value(2));
        mockMvc.perform(get("/api/audit-log").param("q", token).param("action", "GLOBAL_PARAMETER_CHANGED")
                        .param("from", "2026-09-28").param("to", "2026-09-28")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/audit-log").param("q", token).param("from", "2026-09-29")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theAuditLogIsOnlyForAdmins() throws Exception {
        mockMvc.perform(get("/api/audit-log")).andExpect(status().isUnauthorized());
        String teacherToken = loginToken(persist(new co.edu.unicesar.sierap.entity.Teacher("Doc", "Ente",
                DocumentType.CC, document(), email("docente"), passwordEncoder.encode(PASSWORD),
                LocalDateTime.now(clock), faculty)));
        mockMvc.perform(get("/api/audit-log").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ inmutabilidad

    @Test
    void updatingARecordFailsInTheDatabase() {
        AuditLog saved = auditLogRepository.saveAndFlush(new AuditLog(LocalDateTime.now(clock), null, null,
                AuditAction.ACCOUNT_UPDATED, "User", 1L, null, "Original", null));

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE audit_log SET summary = 'Alterado' WHERE id = ?",
                saved.getId())).hasMessageContaining("inmutable");
    }

    @Test
    void deletingARecordFailsInTheDatabase() {
        AuditLog saved = auditLogRepository.saveAndFlush(new AuditLog(LocalDateTime.now(clock), null, null,
                AuditAction.ACCOUNT_UPDATED, "User", 1L, null, "Original", null));

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_log WHERE id = ?", saved.getId()))
                .hasMessageContaining("inmutable");
    }

    // ------------------------------------------------------------ utilidades

    private AuditLog onlyEvent(AuditAction action, Long entityId) {
        var events = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() == action && entityId.equals(log.getEntityId())).toList();
        assertThat(events).hasSize(1);
        return events.get(0);
    }

    private ResultActions extend(String token, Long momentId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(patch("/api/moments/" + momentId + "/extension")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** Conformación del 20 al 30 de septiembre, entregas en octubre y calificación del 2 al 20 de noviembre. */
    private static void windows(Moment moment) {
        moment.setGroupFormationStart(LocalDate.of(2026, 9, 20));
        moment.setGroupFormationEnd(LocalDate.of(2026, 9, 30));
        moment.setSubmissionStart(LocalDate.of(2026, 10, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
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

    private static String document() {
        return "70" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".auditoria" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
