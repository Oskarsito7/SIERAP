package co.edu.unicesar.sierap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
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
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Pruebas de concurrencia de grupos y evaluaciones (auditoría de seguridad, run-1).
 *
 * <p>Una conexión aparte toma el bloqueo de una fila (estudiante o grupo) mientras dos
 * solicitudes HTTP reales llegan a la vez; así se fuerza el mismo cruce que en
 * producción ocurriría por azar. Se comprueba que las operaciones quedan en fila y que
 * el resultado final respeta las reglas: un grupo por estudiante en cada momento, y
 * una evaluación enviada nunca convive con una revocación que no la vio.</p>
 *
 * <p>La clase no es transaccional a propósito: cada solicitud confirma su propia
 * transacción, como en la aplicación real. Los datos llevan sufijos únicos.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GroupConcurrencyIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger((int) (System.nanoTime() % 100_000));

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private ExecutorService pool;
    private Faculty faculty;
    private Moment forming;
    private Moment launched;
    private Teacher teacherA;
    private Teacher teacherB;
    private Student student;

    /** Reloj fijo: 10 de noviembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createData() {
        pool = Executors.newFixedThreadPool(2);
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();
        faculty = new Faculty("K" + n, "Facultad de concurrencia " + n);
        Program program = new Program("KP" + n, "Programa de concurrencia " + n);
        faculty.addProgram(program);

        // En conformación de grupos hoy (para inscribirse y agregar integrantes).
        forming = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 5, "Bajo", 1, 5);
        setWindows(forming, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 28));
        forming.addLearningOutcome(outcome());
        program.addMoment(forming);

        // Lanzado y en ventana de calificación hoy (para guardar y enviar evaluaciones).
        launched = new Moment(period, 2, "Momento 2", "Asignatura 2", 1, 5, "Bajo", 1, 5);
        setWindows(launched, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20));
        launched.addLearningOutcome(outcome());
        launched.launch();
        program.addMoment(launched);
        facultyRepository.saveAndFlush(faculty);

        LocalDateTime now = LocalDateTime.now(clock);
        String hash = passwordEncoder.encode(PASSWORD);
        teacherA = userRepository.saveAndFlush(new Teacher("Docente", "A" + n, DocumentType.CC, "51" + n,
                "concurrencia.da" + n + "@unicesar.edu.co", hash, now, faculty));
        teacherB = userRepository.saveAndFlush(new Teacher("Docente", "B" + n, DocumentType.CC, "52" + n,
                "concurrencia.db" + n + "@unicesar.edu.co", hash, now, faculty));
        student = userRepository.saveAndFlush(new Student("Estudiante", "C" + n, DocumentType.TI, "53" + n,
                "concurrencia.st" + n + "@unicesar.edu.co", hash, now, "KC" + n, 2, program));
    }

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    @Test
    void twoJoinsToDifferentGroupsAtOnceLeaveTheStudentInOneGroup() throws Exception {
        ProjectGroup g1 = group(forming, teacherA, "Grupo 1");
        ProjectGroup g2 = group(forming, teacherB, "Grupo 2");
        String token = loginToken(student);

        int[] statuses = whileRowIsLocked("student", student.getId(), 2,
                () -> status(post("/api/groups/" + g1.getId() + "/join"), token, null),
                () -> status(post("/api/groups/" + g2.getId() + "/join"), token, null));

        // La segunda inscripción ve la primera y cambia de grupo en un solo paso.
        assertThat(statuses).containsExactly(200, 200);
        assertThat(memberships(forming)).isEqualTo(1);
    }

    @Test
    void twoTeachersAddingTheSameStudentAtOnceLeaveItInOneGroup() throws Exception {
        ProjectGroup g1 = group(forming, teacherA, "Grupo 1");
        ProjectGroup g2 = group(forming, teacherB, "Grupo 2");
        String tokenA = loginToken(teacherA);
        String tokenB = loginToken(teacherB);
        Map<String, Object> body = Map.of("studentId", student.getId());

        int[] statuses = whileRowIsLocked("student", student.getId(), 2,
                () -> status(post("/api/groups/" + g1.getId() + "/members"), tokenA, body),
                () -> status(post("/api/groups/" + g2.getId() + "/members"), tokenB, body));

        assertThat(statuses).containsExactlyInAnyOrder(200, 422);
        assertThat(memberships(forming)).isEqualTo(1);
    }

    @Test
    void submittingAnEvaluationWaitsForARevocationThatHoldsTheGroup() throws Exception {
        ProjectGroup group = group(launched, teacherA, "Grupo calificado");
        group.addMember(student);
        projectGroupRepository.saveAndFlush(group);
        String token = loginToken(teacherA);
        EvaluationCriterion criterion = launched.getLearningOutcomes().get(0).getCriteria().get(0);
        Map<String, Object> draft = Map.of("levels", List.of(Map.of("criterionId", criterion.getId(),
                "levelId", criterion.getLevels().get(0).getId())));
        assertThat(status(put("/api/groups/" + group.getId() + "/evaluation"), token, draft)).isEqualTo(200);

        // Mientras otra transacción (una reasignación o un retiro) tiene el grupo, el envío
        // espera: así la validación de esa revocación nunca ve un estado a medias.
        int[] statuses = whileRowIsLocked("project_group", group.getId(), 1,
                () -> status(post("/api/groups/" + group.getId() + "/evaluation/submit"), token, null));

        assertThat(statuses).containsExactly(200);
    }

    @Test
    void removingAMemberWaitsForAnEvaluationThatHoldsTheGroup() throws Exception {
        ProjectGroup group = group(launched, teacherA, "Grupo con retiro");
        group.addMember(student);
        projectGroupRepository.saveAndFlush(group);
        String token = loginToken(teacherA);

        int[] statuses = whileRowIsLocked("project_group", group.getId(), 1,
                () -> status(delete("/api/groups/" + group.getId() + "/members/" + student.getId()), token,
                        null));

        assertThat(statuses).containsExactly(204);
    }

    // ----------------------------------------------------------------- utilidades

    /**
     * Toma {@code SELECT ... FOR UPDATE} sobre una fila, lanza las solicitudes y espera
     * a que todas queden bloqueadas por ese candado; luego lo suelta y devuelve los
     * estados HTTP en el orden en que se lanzaron. Si alguna no se bloquea, la prueba
     * falla: significa que esa operación no toma el bloqueo esperado.
     */
    @SafeVarargs
    private int[] whileRowIsLocked(String table, Long id, int expectedWaiting, Callable<Integer>... requests)
            throws Exception {
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try (PreparedStatement lock = holder.prepareStatement(
                    "select id from " + table + " where id = ? for update")) {
                lock.setLong(1, id);
                lock.executeQuery();
            }
            List<Future<Integer>> futures = Arrays.stream(requests).map(pool::submit).toList();
            int waiting = 0;
            for (int i = 0; i < 200 && waiting < expectedWaiting; i++) {
                Thread.sleep(25);
                waiting = jdbc.queryForObject("select count(*) from pg_locks where not granted", Integer.class);
            }
            assertThat(waiting).as("solicitudes esperando el bloqueo de %s", table)
                    .isGreaterThanOrEqualTo(expectedWaiting);
            assertThat(futures).noneMatch(Future::isDone);
            holder.rollback();
            int[] statuses = new int[futures.size()];
            for (int i = 0; i < statuses.length; i++) {
                statuses[i] = futures.get(i).get(30, TimeUnit.SECONDS);
            }
            return statuses;
        }
    }

    private int status(MockHttpServletRequestBuilder request, String token, Map<String, ?> body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    private int memberships(Moment moment) {
        return jdbc.queryForObject("select count(*) from project_group_member m join project_group g "
                + "on g.id = m.project_group_id where g.moment_id = ? and m.student_id = ?", Integer.class,
                moment.getId(), student.getId());
    }

    private ProjectGroup group(Moment moment, Teacher teacher, String name) {
        return projectGroupRepository.saveAndFlush(new ProjectGroup(moment, teacher, name));
    }

    private String loginToken(User user) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static LearningOutcome outcome() {
        EvaluationCriterion criterion = new EvaluationCriterion("Problema", 1);
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal("1.00"), 1));
        LearningOutcome outcome = new LearningOutcome("RAP1", "Descripción de RAP1");
        outcome.addCriterion(criterion);
        return outcome;
    }

    private static void setWindows(Moment moment, LocalDate formationStart, LocalDate formationEnd,
                                   LocalDate submissionStart, LocalDate submissionEnd, LocalDate gradingStart,
                                   LocalDate gradingEnd) {
        moment.setGroupFormationStart(formationStart);
        moment.setGroupFormationEnd(formationEnd);
        moment.setSubmissionStart(submissionStart);
        moment.setSubmissionEnd(submissionEnd);
        moment.setGradingStart(gradingStart);
        moment.setGradingEnd(gradingEnd);
    }
}
