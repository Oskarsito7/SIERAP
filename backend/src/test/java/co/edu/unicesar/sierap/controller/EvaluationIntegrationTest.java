package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
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
 * Prueba de integración de la calificación de los grupos (Etapa 6, {@code docs/07}
 * "Evaluation") contra PostgreSQL real, con un reloj fijo.
 *
 * <p>Hoy es el 10 de noviembre de 2026. Cada prueba crea una facultad con un programa y
 * tres momentos, y se revierte al terminar:</p>
 * <ul>
 *   <li>{@code launched}: lanzado, calificación del 2 al 20 de noviembre, con dos RAP de
 *       un criterio cada uno: "Problema" (máx. 1,00) y "Sustentación" (máx. 2,00). Su
 *       grupo tiene a Ana y a Luis, un evaluador externo asignado y una entrega.</li>
 *   <li>{@code closed}: lanzado, la calificación cerró el 20 de agosto.</li>
 *   <li>{@code configuring}: todavía en configuración.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class EvaluationIntegrationTest {

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
    private Moment launched;
    private Moment closed;
    private Moment configuring;
    private EvaluationCriterion problem;
    private EvaluationCriterion defense;
    private Teacher teacher;
    private ExternalEvaluator evaluator;
    private Student ana;
    private Student luis;
    private ProjectGroup group;
    private String teacherToken;

    /** Reloj fijo: 10 de noviembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createMomentsAndGroup() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();
        faculty = new Faculty("V" + n, "Facultad de evaluación " + n);
        Program program = new Program("VP" + n, "Programa de evaluación " + n);
        faculty.addProgram(program);

        launched = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 2, 4);
        setWindows(launched, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20));
        launched.addLearningOutcome(outcome("RAP1", "Problema", "1.00"));
        launched.addLearningOutcome(outcome("RAP2", "Sustentación", "2.00"));
        launched.launch();
        program.addMoment(launched);

        closed = new Moment(period, 2, "Momento 2", "Asignatura 2", 1, 3, "Bajo", 2, 4);
        setWindows(closed, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 20));
        closed.addLearningOutcome(outcome("RAP1", "Problema", "1.00"));
        closed.launch();
        program.addMoment(closed);

        configuring = new Moment(period, 3, "Momento 3", "Asignatura 3", 1, 3, "Bajo", 2, 4);
        configuring.addLearningOutcome(outcome("RAP1", "Problema", "1.00"));
        program.addMoment(configuring);
        facultyRepository.saveAndFlush(faculty);
        problem = launched.getLearningOutcomes().get(0).getCriteria().get(0);
        defense = launched.getLearningOutcomes().get(1).getCriteria().get(0);

        teacher = teacher();
        evaluator = externalEvaluator();
        ana = student("Ana", "Gómez", program);
        luis = student("Luis", "Martínez", program);
        group = new ProjectGroup(launched, teacher, "Grupo 1");
        group.addMember(ana);
        group.addMember(luis);
        group.addEvaluator(evaluator);
        group.addSubmission(new Submission(1, new Submission.StoredFile("groups/v1.zip", "v1.zip", 10),
                LocalDateTime.of(2026, 10, 20, 9, 0), false));
        group = projectGroupRepository.saveAndFlush(group);
        teacherToken = loginToken(teacher);
    }

    // ------------------------------------------------------------ formulario y borrador

    @Test
    void theFormStartsEmptyWithTheStructureOfTheMoment() throws Exception {
        mockMvc.perform(get(evaluationPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.status").value(nullValue()))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.submissionToGrade.versionNumber").value(1))
                .andExpect(jsonPath("$.learningOutcomes[*].code", contains("RAP1", "RAP2")))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levels", hasSize(2)))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levelId").value(nullValue()))
                .andExpect(jsonPath("$.members[*].fullName", contains("Ana Gómez", "Luis Martínez")))
                .andExpect(jsonPath("$.members[0].totalScore").value(0.0))
                .andExpect(jsonPath("$.totalScore").value(0.0))
                .andExpect(jsonPath("$.maxScore").value(3.0))
                .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void aDraftCanBePartialAndIsSubmittedOnlyWhenComplete() throws Exception {
        save(teacherToken, Map.of("levels", List.of(level(problem, "Alto")), "comment", " Buen trabajo. "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.comment").value("Buen trabajo."))
                .andExpect(jsonPath("$.totalScore").value(1.0))
                .andExpect(jsonPath("$.complete").value(false));

        submit(teacherToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Falta calificar 1 criterio."));

        save(teacherToken, Map.of("levels", List.of(level(defense, "Bajo"))))
                .andExpect(jsonPath("$.comment").value("Buen trabajo."))
                .andExpect(jsonPath("$.learningOutcomes[0].criteria[0].levelId").value(levelId(problem, "Alto")))
                .andExpect(jsonPath("$.complete").value(true));
        submit(teacherToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // Enviada, se puede corregir y sigue enviada.
        save(teacherToken, Map.of("levels", List.of(level(defense, "Alto"))))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.totalScore").value(3.0));
    }

    @Test
    void submittingWithoutAnyGradeSaysHowManyCriteriaAreMissing() throws Exception {
        submit(teacherToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Falta calificar 2 criterios."));
    }

    // ------------------------------------------------------------ ajustes individuales

    @Test
    void adjustmentsChangeTheGradeOfOneStudentAndAreReplacedAsAWhole() throws Exception {
        save(teacherToken, Map.of(
                "levels", List.of(level(problem, "Alto"), level(defense, "Alto")),
                "adjustments", List.of(adjustment(ana, defense, "Bajo"), adjustment(luis, problem, "Alto"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(3.0))
                .andExpect(jsonPath("$.members[0].adjustments", hasSize(1)))
                .andExpect(jsonPath("$.members[0].adjustments[0].criterionId").value(defense.getId()))
                .andExpect(jsonPath("$.members[0].adjustments[0].levelId").value(levelId(defense, "Bajo")))
                .andExpect(jsonPath("$.members[0].totalScore").value(1.0))
                // Un ajuste igual al nivel del grupo se descarta.
                .andExpect(jsonPath("$.members[1].adjustments", empty()))
                .andExpect(jsonPath("$.members[1].totalScore").value(3.0));

        // Sin "adjustments" se conservan; con una lista vacía se borran.
        save(teacherToken, Map.of("comment", "Revisado"))
                .andExpect(jsonPath("$.members[0].adjustments", hasSize(1)));
        save(teacherToken, Map.of("adjustments", List.of()))
                .andExpect(jsonPath("$.members[0].adjustments", empty()))
                .andExpect(jsonPath("$.members[0].totalScore").value(3.0));
    }

    @Test
    void removingAMemberDeletesTheirAdjustmentsInDrafts() throws Exception {
        save(teacherToken, Map.of("levels", List.of(level(problem, "Alto")),
                "adjustments", List.of(adjustment(ana, problem, "Bajo"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/groups/" + group.getId() + "/members/" + ana.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(evaluationPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(jsonPath("$.members[*].fullName", contains("Luis Martínez")));
    }

    @Test
    void levelsCriteriaAndStudentsAreValidated() throws Exception {
        Student outsider = student("Otro", "Estudiante", launched.getProgram());
        EvaluationCriterion foreign = closed.getLearningOutcomes().get(0).getCriteria().get(0);

        save(teacherToken, Map.of("levels", List.of(Map.of("criterionId", problem.getId(),
                "levelId", levelId(defense, "Alto")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El nivel " + levelId(defense, "Alto")
                        + " no es del criterio \"Problema\"."));
        save(teacherToken, Map.of("levels", List.of(level(foreign, "Alto"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El criterio " + foreign.getId() + " no es de este momento."));
        save(teacherToken, Map.of("adjustments", List.of(adjustment(outsider, problem, "Alto"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El estudiante " + outsider.getId()
                        + " no es integrante del grupo."));
        save(teacherToken, Map.of("levels", List.of(Map.of("criterionId", problem.getId()))))
                .andExpect(status().isBadRequest());

        // Nada de lo anterior creó la evaluación.
        mockMvc.perform(get(evaluationPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(jsonPath("$.id").value(nullValue()));
    }

    // ------------------------------------------------------------ quién y cuándo

    @Test
    void eachEvaluatorHasTheirOwnEvaluationAndOutsidersCannotGrade() throws Exception {
        String evaluatorToken = loginToken(evaluator);
        save(teacherToken, Map.of("levels", List.of(level(problem, "Alto")))).andExpect(status().isOk());
        save(evaluatorToken, Map.of("levels", List.of(level(problem, "Bajo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));

        ExternalEvaluator unassigned = externalEvaluator();
        for (User outsider : new User[] {teacher(), unassigned}) {
            save(loginToken(outsider), Map.of("levels", List.of(level(problem, "Alto"))))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(evaluationPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ana))))
                .andExpect(status().isForbidden());
    }

    @Test
    void gradingOnlyHappensBetweenTheGroupEvaluationDateAndTheEndOfTheWindow() throws Exception {
        group.setEvaluationDate(LocalDate.of(2026, 11, 15));
        projectGroupRepository.saveAndFlush(group);

        mockMvc.perform(get(evaluationPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(jsonPath("$.canEdit").value(false));
        save(teacherToken, Map.of("levels", List.of(level(problem, "Alto"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("La evaluación de este grupo es el 2026-11-15: antes no se puede calificar."));

        ProjectGroup closedGroup = projectGroupRepository.saveAndFlush(new ProjectGroup(closed, teacher, "Cerrado"));
        submitFor(closedGroup)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La ventana de calificación cerró el 2026-08-20."));

        ProjectGroup draftGroup = projectGroupRepository.saveAndFlush(new ProjectGroup(configuring, teacher, "Nuevo"));
        submitFor(draftGroup)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El momento todavía no se ha lanzado: aún no se puede calificar."));
    }

    // ------------------------------------------------------------ lista de evaluaciones

    @Test
    void theTeacherSeesEveryEvaluationAndAnExternalEvaluatorOnlyTheirOwn() throws Exception {
        String evaluatorToken = loginToken(evaluator);
        save(evaluatorToken, Map.of("levels", List.of(level(problem, "Bajo")), "comment", "Mejorar")).andExpect(
                status().isOk());
        save(teacherToken, Map.of("levels", List.of(level(problem, "Alto"), level(defense, "Alto"))));
        submit(teacherToken).andExpect(status().isOk());

        mockMvc.perform(get(evaluationsPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].evaluator.kind", contains("Docente", "Empresario")))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].totalScore").value(3.0))
                .andExpect(jsonPath("$[1].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].comment").value("Mejorar"));

        mockMvc.perform(get(evaluationsPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(evaluatorToken)))
                .andExpect(jsonPath("$[*].evaluator.kind", contains("Empresario")));

        Admin ownAdmin = persist(new Admin("Admin", "Propio", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        mockMvc.perform(get(evaluationsPath(group)).header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ownAdmin))))
                .andExpect(jsonPath("$", hasSize(2)));

        Faculty other = facultyRepository.saveAndFlush(new Faculty("W" + SEQUENCE.incrementAndGet(), "Otra facultad"));
        Admin foreignAdmin = persist(new Admin("Admin", "Ajeno", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), other));
        for (User outsider : new User[] {foreignAdmin, teacher()}) {
            mockMvc.perform(get(evaluationsPath(group))
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(outsider))))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions save(String token, Map<String, ?> body) throws Exception {
        return perform(put(evaluationPath(group)), token, body);
    }

    private ResultActions submit(String token) throws Exception {
        return mockMvc.perform(post(evaluationPath(group) + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions submitFor(ProjectGroup target) throws Exception {
        return mockMvc.perform(post(evaluationPath(target) + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, Map<String, ?> body)
            throws Exception {
        return mockMvc.perform(request
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static String evaluationPath(ProjectGroup target) {
        return "/api/groups/" + target.getId() + "/evaluation";
    }

    private static String evaluationsPath(ProjectGroup target) {
        return "/api/groups/" + target.getId() + "/evaluations";
    }

    private static Map<String, Long> level(EvaluationCriterion criterion, String name) {
        return Map.of("criterionId", criterion.getId(), "levelId", levelId(criterion, name));
    }

    private static Map<String, Long> adjustment(Student student, EvaluationCriterion criterion, String name) {
        Map<String, Long> body = new HashMap<>(level(criterion, name));
        body.put("studentId", student.getId());
        return body;
    }

    private static Long levelId(EvaluationCriterion criterion, String name) {
        return criterion.getLevels().stream()
                .filter(level -> level.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static LearningOutcome outcome(String code, String criterionName, String max) {
        EvaluationCriterion criterion = new EvaluationCriterion(criterionName, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, BigDecimal.ZERO, 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(max), 2));
        LearningOutcome outcome = new LearningOutcome(code, "Descripción de " + code);
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

    private Teacher teacher() {
        return persist(new Teacher("Docente", "Prueba " + SEQUENCE.incrementAndGet(), DocumentType.CC, document(),
                email("docente"), passwordEncoder.encode(PASSWORD), now(), faculty));
    }

    private ExternalEvaluator externalEvaluator() {
        ExternalEvaluator created = new ExternalEvaluator("Evaluador", "Prueba", DocumentType.CC, document(),
                "evaluador.calificacion" + SEQUENCE.incrementAndGet() + "@correo.com",
                passwordEncoder.encode(PASSWORD), now(),
                evaluatorTypeRepository.findByName("Empresario").orElseThrow());
        created.setCreatedBy(teacher);
        return persist(created);
    }

    private Student student(String firstNames, String lastNames, Program program) {
        String code = "V" + SEQUENCE.incrementAndGet();
        return persist(new Student(firstNames, lastNames, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), code, 2, program));
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
        return "36" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".calificacion" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
