package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de las operaciones del docente sobre sus grupos (Etapa 4,
 * {@code docs/07} "Groups") contra PostgreSQL real: alcance por rol y ventanas del
 * momento calculadas con un reloj fijo.
 *
 * <p>Hoy es el 28 de septiembre de 2026. Cada prueba crea una facultad con un programa y
 * tres momentos, y se revierte al terminar:</p>
 * <ul>
 *   <li>{@code configuring}: en configuración, semestres 1 a 3, conformación abierta;
 *       entregas en octubre y calificación del 2 al 20 de noviembre.</li>
 *   <li>{@code launched}: lanzado, con la ventana de entregas abierta.</li>
 *   <li>{@code closed}: lanzado, con la ventana de calificación ya cerrada (agosto).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class GroupIntegrationTest {

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

    private Faculty faculty;
    private Faculty otherFaculty;
    private Program program;
    private Program otherProgram;
    private Moment configuring;
    private Moment launched;
    private Moment closed;
    private Moment foreignMoment;
    private Teacher teacher;
    private String teacherToken;

    /** Reloj fijo: 28 de septiembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createFacultiesMomentsAndTeacher() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();

        faculty = new Faculty("G" + n, "Facultad de grupos " + n);
        program = new Program("GP" + n, "Programa de grupos " + n);
        otherProgram = new Program("GO" + n, "Otro programa " + n);
        faculty.addProgram(program);
        faculty.addProgram(otherProgram);

        configuring = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 3, 5);
        setWindows(configuring, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        configuring.addLearningOutcome(outcome());
        program.addMoment(configuring);

        launched = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        setWindows(launched, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));
        launched.addLearningOutcome(outcome());
        launched.launch();
        program.addMoment(launched);

        closed = new Moment(period, 3, "Momento 3", "Asignatura 3", 7, 9, "Alto", 2, 4);
        closed.setGroupFormationStart(LocalDate.of(2026, 8, 3));
        closed.setGroupFormationEnd(LocalDate.of(2026, 8, 5));
        closed.setSubmissionStart(LocalDate.of(2026, 8, 6));
        closed.setSubmissionEnd(LocalDate.of(2026, 8, 10));
        closed.setGradingStart(LocalDate.of(2026, 8, 11));
        closed.setGradingEnd(LocalDate.of(2026, 8, 20));
        closed.addLearningOutcome(outcome());
        closed.launch();
        program.addMoment(closed);
        facultyRepository.saveAndFlush(faculty);

        otherFaculty = new Faculty("H" + n, "Otra facultad " + n);
        Program foreignProgram = new Program("HP" + n, "Programa ajeno " + n);
        otherFaculty.addProgram(foreignProgram);
        foreignMoment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        foreignProgram.addMoment(foreignMoment);
        facultyRepository.saveAndFlush(otherFaculty);

        teacher = teacher(faculty);
        teacherToken = loginToken(teacher);
    }

    // ------------------------------------------------------------ crear y consultar

    @Test
    void theTeacherEnablesAGroupAndOnlyTheySeeIt() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");

        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Grupo 1")))
                .andExpect(jsonPath("$[0].teacher.id").value(teacher.getId()))
                .andExpect(jsonPath("$[0].minMembers").value(3))
                .andExpect(jsonPath("$[0].memberCount").value(0));

        String otherToken = loginToken(teacher(faculty));
        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupsCannotBeEnabledOnceTheMomentWasLaunched() throws Exception {
        perform(post("/api/groups"), teacherToken, Map.of("momentId", launched.getId(), "name", "Tarde"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("ya fue lanzado")));
    }

    @Test
    void groupsCannotBeEnabledInAMomentOfAnotherFaculty() throws Exception {
        perform(post("/api/groups"), teacherToken, Map.of("momentId", foreignMoment.getId(), "name", "Grupo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTeacherCannotRepeatTheNameOfTheirGroupInTheMoment() throws Exception {
        createGroup(teacherToken, configuring, "Grupo 1");

        perform(post("/api/groups"), teacherToken, Map.of("momentId", configuring.getId(), "name", " grupo 1 "))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theAdminSeesTheGroupsOfTheirFacultyOnly() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        String adminToken = loginToken(persist(new Admin("Admin", "Prueba", DocumentType.CC, document(),
                email("admin"), passwordEncoder.encode(PASSWORD), now(), faculty)));
        String foreignAdminToken = loginToken(persist(new Admin("Admin", "Ajeno", DocumentType.CC, document(),
                email("admin"), passwordEncoder.encode(PASSWORD), now(), otherFaculty)));

        mockMvc.perform(get("/api/groups").param("momentId", configuring.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains((int) id)));
        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(foreignAdminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/groups").param("momentId", configuring.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(foreignAdminToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theSuperAdminSeesTheGroupsOfAnyMoment() throws Exception {
        createGroup(teacherToken, configuring, "Grupo 1");
        String token = loginToken(persist(new SuperAdmin("Super", "Prueba", DocumentType.CC, document(),
                email("super"), passwordEncoder.encode(PASSWORD), now())));

        mockMvc.perform(get("/api/groups").param("momentId", configuring.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ------------------------------------------------------------ editar y eliminar

    @Test
    void datesMustFallInsideTheirWindowsAndNotBeforeToday() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");

        perform(patch("/api/groups/" + id), teacherToken,
                Map.of("submissionDeadline", "2026-10-20", "evaluationDate", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionDeadline").value("2026-10-20"))
                .andExpect(jsonPath("$.evaluationDate").value("2026-11-10"));
        perform(patch("/api/groups/" + id), teacherToken, Map.of("submissionDeadline", "2026-11-05"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("ventana de entregas")));
        perform(patch("/api/groups/" + id), teacherToken, Map.of("evaluationDate", "2026-10-15"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("ventana de calificación")));
    }

    @Test
    void aDeadlineCannotBeEarlierThanToday() throws Exception {
        ProjectGroup group = save(new ProjectGroup(launched, teacher, "Grupo 1"));

        perform(patch("/api/groups/" + group.getId()), teacherToken, Map.of("submissionDeadline", "2026-09-10"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("anterior a hoy")));
    }

    @Test
    void theNameOnlyChangesBeforeTheLaunch() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        ProjectGroup launchedGroup = save(new ProjectGroup(launched, teacher, "Grupo 1"));

        perform(patch("/api/groups/" + id), teacherToken, Map.of("name", "Grupo A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grupo A"));
        perform(patch("/api/groups/" + launchedGroup.getId()), teacherToken, Map.of("name", "Grupo B"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void onlyTheOwnTeacherEditsTheGroup() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        String otherToken = loginToken(teacher(faculty));

        perform(patch("/api/groups/" + id), otherToken, Map.of("name", "Mío"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anEmptyGroupIsDeletedOnlyBeforeTheLaunch() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        ProjectGroup launchedGroup = save(new ProjectGroup(launched, teacher, "Grupo 1"));

        mockMvc.perform(delete("/api/groups/" + launchedGroup.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aGroupWithMembersCannotBeDeleted() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        addMember(id, student(program, 2)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("integrantes")));
    }

    @Test
    void theTeacherAndTheMembersWriteTheProjectTitle() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        Student member = student(program, 2);
        addMember(id, member).andExpect(status().isOk());

        perform(put("/api/groups/" + id + "/project-title"), teacherToken, Map.of("projectTitle", "SIERAP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectTitle").value("SIERAP"));
        perform(put("/api/groups/" + id + "/project-title"), loginToken(member),
                Map.of("projectTitle", "SIERAP 2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myGroup").value(true));
        perform(put("/api/groups/" + id + "/project-title"), loginToken(student(program, 2)),
                Map.of("projectTitle", "Ajeno"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ integrantes

    @Test
    void listsTheStudentsOfTheProgramInRangeWithoutAGroup() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        Student inRange = student(program, 2);
        Student alreadyInGroup = student(program, 3);
        student(program, 5);
        student(otherProgram, 2);
        addMember(id, alreadyInGroup).andExpect(status().isOk());

        mockMvc.perform(get("/api/moments/" + configuring.getId() + "/students-without-group")
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(inRange.getId().intValue())))
                .andExpect(jsonPath("$[0].currentSemester").value(2));
    }

    @Test
    void theTeacherAddsAStudentOutOfRangeAndAboveTheMaximumAfterTheLaunch() throws Exception {
        ProjectGroup group = save(new ProjectGroup(launched, teacher, "Grupo 1"));
        for (int i = 0; i < 4; i++) {
            addMember(group.getId(), student(program, 5)).andExpect(status().isOk());
        }

        addMember(group.getId(), student(program, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(5))
                .andExpect(jsonPath("$.full").value(true));
    }

    @Test
    void aStudentCannotBeInTwoGroupsOfTheSameMoment() throws Exception {
        long first = createGroup(teacherToken, configuring, "Grupo 1");
        long second = createGroup(teacherToken, configuring, "Grupo 2");
        Student student = student(program, 2);
        addMember(first, student).andExpect(status().isOk());

        addMember(second, student)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Grupo 1")));
    }

    @Test
    void onlyStudentsOfTheMomentProgramCanBeAdded() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");

        addMember(id, student(otherProgram, 2)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void nothingChangesAfterTheGradingWindowClosed() throws Exception {
        ProjectGroup group = save(new ProjectGroup(closed, teacher, "Grupo 1"));

        addMember(group.getId(), student(program, 8))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("2026-08-20")));
    }

    @Test
    void membersCannotBeRemovedOnceAnEvaluationWasSubmitted() throws Exception {
        ProjectGroup group = save(new ProjectGroup(launched, teacher, "Grupo 1"));
        Student first = student(program, 5);
        Student second = student(program, 5);
        addMember(group.getId(), first).andExpect(status().isOk());
        addMember(group.getId(), second).andExpect(status().isOk());

        mockMvc.perform(delete("/api/groups/" + group.getId() + "/members/" + first.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNoContent());

        ProjectGroup reloaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        Evaluation evaluation = new Evaluation(teacher, now());
        reloaded.addEvaluation(evaluation);
        evaluation.submit();
        projectGroupRepository.saveAndFlush(reloaded);

        mockMvc.perform(delete("/api/groups/" + group.getId() + "/members/" + second.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("evaluación enviada")));
    }

    // ------------------------------------------------------------ evaluadores externos

    @Test
    void findsAnActiveExternalEvaluatorOnlyByExactEmailOrDocument() throws Exception {
        ExternalEvaluator evaluator = externalEvaluator();

        mockMvc.perform(get("/api/external-evaluators/lookup").param("q", evaluator.getEmail().toUpperCase())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evaluator.getId()));
        mockMvc.perform(get("/api/external-evaluators/lookup").param("q", evaluator.getDocumentNumber())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/external-evaluators/lookup").param("q", "evaluador")
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNotFound());

        evaluator.setActive(false);
        userRepository.saveAndFlush(evaluator);
        mockMvc.perform(get("/api/external-evaluators/lookup").param("q", evaluator.getDocumentNumber())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAssignedEvaluatorSeesTheGroupAndCannotBeAssignedTwice() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        ExternalEvaluator evaluator = externalEvaluator();

        assignEvaluator(id, evaluator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluators[0].id").value(evaluator.getId()))
                .andExpect(jsonPath("$.evaluators[0].evaluatorType").value("Empresario"));
        assignEvaluator(id, evaluator).andExpect(status().isUnprocessableEntity());

        String evaluatorToken = loginToken(evaluator);
        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bearer(evaluatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains((int) id)));
        mockMvc.perform(get("/api/groups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(evaluatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void aDeactivatedEvaluatorCannotBeAssigned() throws Exception {
        long id = createGroup(teacherToken, configuring, "Grupo 1");
        ExternalEvaluator evaluator = externalEvaluator();
        evaluator.setActive(false);
        userRepository.saveAndFlush(evaluator);

        assignEvaluator(id, evaluator)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("desactivado")));
    }

    @Test
    void anEvaluatorWhoStartedGradingCannotBeRemoved() throws Exception {
        ProjectGroup group = save(new ProjectGroup(launched, teacher, "Grupo 1"));
        ExternalEvaluator started = externalEvaluator();
        ExternalEvaluator idle = externalEvaluator();
        assignEvaluator(group.getId(), started).andExpect(status().isOk());
        assignEvaluator(group.getId(), idle).andExpect(status().isOk());

        ProjectGroup reloaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        reloaded.addEvaluation(new Evaluation(started, now()));
        projectGroupRepository.saveAndFlush(reloaded);

        mockMvc.perform(delete("/api/groups/" + group.getId() + "/evaluators/" + started.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/groups/" + group.getId() + "/evaluators/" + idle.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------ utilidades

    private long createGroup(String token, Moment moment, String name) throws Exception {
        String body = perform(post("/api/groups"), token, Map.of("momentId", moment.getId(), "name", name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions addMember(long groupId, Student student) throws Exception {
        return perform(post("/api/groups/" + groupId + "/members"), teacherToken,
                Map.of("studentId", student.getId()));
    }

    private ResultActions assignEvaluator(long groupId, ExternalEvaluator evaluator) throws Exception {
        return perform(post("/api/groups/" + groupId + "/evaluators"), teacherToken,
                Map.of("evaluatorId", evaluator.getId()));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, Map<String, ?> body)
            throws Exception {
        return mockMvc.perform(request
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ProjectGroup save(ProjectGroup group) {
        return projectGroupRepository.saveAndFlush(group);
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

    private Teacher teacher(Faculty ofFaculty) {
        return persist(new Teacher("Docente", "Prueba", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), ofFaculty));
    }

    private Student student(Program ofProgram, int semester) {
        String code = "S" + SEQUENCE.incrementAndGet();
        return persist(new Student("Estudiante", "Prueba " + code, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), code, semester, ofProgram));
    }

    private ExternalEvaluator externalEvaluator() {
        ExternalEvaluator evaluator = new ExternalEvaluator("Evaluador", "Prueba", DocumentType.CC, document(),
                "evaluador.grupos" + SEQUENCE.incrementAndGet() + "@correo.com", passwordEncoder.encode(PASSWORD),
                now(), evaluatorTypeRepository.findByName("Empresario").orElseThrow());
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
        return "33" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".grupos" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
