package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.EvaluatorType;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de los resultados (Etapa 6, {@code docs/07} "Results", ADR-018)
 * contra PostgreSQL real y con un reloj fijo: hoy es el 25 de noviembre de 2026.
 *
 * <p>Los momentos pesan docente 60 %, empresario 20 % y egresado 20 %, y tienen dos RAP:
 * RAP1 (máximo 1,00) y RAP2 (máximo 2,00).</p>
 * <ul>
 *   <li>{@code closed}: su calificación cerró el 20 de noviembre. El grupo {@code graded}
 *       tiene la evaluación enviada del docente (3,00, con un ajuste para Ana en RAP2), la
 *       de un empresario (2,00), la de un jurado cuyo tipo no tiene peso en el momento y
 *       un borrador de otro empresario. Sin egresado, el 60/20 pasa a 75/25.</li>
 *   <li>{@code open}: su calificación sigue abierta hasta el 27 de noviembre.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ResultIntegrationTest {

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
    private Program program;
    private Moment closed;
    private Moment open;
    private Teacher teacher;
    private ExternalEvaluator business;
    private Student ana;
    private Student beto;
    private ProjectGroup graded;

    /** Reloj fijo: 25 de noviembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 11, 25).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createMomentsAndGradedGroup() {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();
        EvaluatorType businessType = evaluatorTypeRepository.findByName("Empresario").orElseThrow();
        EvaluatorType alumnusType = evaluatorTypeRepository.findByName("Egresado").orElseThrow();
        EvaluatorType juryType = evaluatorTypeRepository.saveAndFlush(
                new EvaluatorType("Jurado " + n, "Tipo sin peso en los momentos de la prueba"));

        faculty = new Faculty("R" + n, "Facultad de resultados " + n);
        program = new Program("RP" + n, "Programa de resultados " + n);
        faculty.addProgram(program);
        closed = launchedMoment(period, 1, LocalDate.of(2026, 11, 20), businessType, alumnusType);
        open = launchedMoment(period, 2, LocalDate.of(2026, 11, 27), businessType, alumnusType);
        program.addMoment(closed);
        program.addMoment(open);
        facultyRepository.saveAndFlush(faculty);

        teacher = persist(new Teacher("Docente", "Resultados", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        business = externalEvaluator(businessType);
        ExternalEvaluator draftBusiness = externalEvaluator(businessType);
        ExternalEvaluator jury = externalEvaluator(juryType);
        ana = student("Ana", "Alba");
        beto = student("Beto", "Bravo");

        graded = new ProjectGroup(closed, teacher, "Grupo calificado");
        graded.addMember(beto);
        graded.addMember(ana);
        graded.addEvaluator(business);
        graded.addEvaluator(draftBusiness);
        graded.addEvaluator(jury);

        // Docente: RAP1 1,00 + RAP2 2,00 = 3,00; Ana tiene RAP2 en 0,50.
        Evaluation byTeacher = evaluation(graded, teacher, level(closed, 0, 1), level(closed, 1, 1));
        byTeacher.adjust(ana, level(closed, 1, 0));
        byTeacher.setComment("Buen trabajo en equipo.");
        byTeacher.submit();
        // Empresario: RAP1 0,00 + RAP2 2,00 = 2,00.
        Evaluation byBusiness = evaluation(graded, business, level(closed, 0, 0), level(closed, 1, 1));
        byBusiness.setComment("Mejoren la presentación.");
        byBusiness.submit();
        // Jurado: su tipo no tiene peso en el momento, así que no cuenta aunque esté enviada.
        evaluation(graded, jury, level(closed, 0, 0), level(closed, 1, 0)).submit();
        // Borrador: no cuenta.
        evaluation(graded, draftBusiness, level(closed, 0, 0), level(closed, 1, 0));
        graded = projectGroupRepository.saveAndFlush(graded);
    }

    // ------------------------------------------------------------ resultados del grupo

    @Test
    void theTeacherSeesTheWeightedGroupAndIndividualGrades() throws Exception {
        groupResults(loginToken(teacher), graded)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(graded.getId()))
                .andExpect(jsonPath("$.final").value(true))
                .andExpect(jsonPath("$.maxScore").value(3.0))
                // 0,75 × 3,00 + 0,25 × 2,00 = 2,75
                .andExpect(jsonPath("$.groupGrade").value(2.75))
                .andExpect(jsonPath("$.groupLearningOutcomes[*].code", contains("RAP1", "RAP2")))
                .andExpect(jsonPath("$.groupLearningOutcomes[*].score", contains(0.75, 2.0)))
                .andExpect(jsonPath("$.groupLearningOutcomes[*].maxScore", contains(1.0, 2.0)))
                .andExpect(jsonPath("$.members[*].fullName", contains("Ana Alba", "Beto Bravo")))
                // Ana: 0,75 × 1,50 + 0,25 × 2,00 = 1,625 → 1,63; por RAP 0,75 y 0,875 → 0,88
                .andExpect(jsonPath("$.members[0].individualGrade").value(1.63))
                .andExpect(jsonPath("$.members[0].learningOutcomes[*].score", contains(0.75, 0.88)))
                .andExpect(jsonPath("$.members[1].individualGrade").value(2.75))
                .andExpect(jsonPath("$.members[1].learningOutcomes[*].score", contains(0.75, 2.0)))
                .andExpect(jsonPath("$.submittedEvaluations").value(3))
                .andExpect(jsonPath("$.pendingEvaluations").value(1));
    }

    @Test
    void whileGradingIsOpenTheResultsAreAPreview() throws Exception {
        ProjectGroup inProgress = new ProjectGroup(open, teacher, "Grupo en calificación");
        inProgress.addMember(student("Caro", "Castro"));
        inProgress.addEvaluator(business);
        // Docente: RAP1 1,00 + RAP2 0,50 = 1,50; el empresario no ha enviado.
        evaluation(inProgress, teacher, level(open, 0, 1), level(open, 1, 0)).submit();
        inProgress = projectGroupRepository.saveAndFlush(inProgress);

        groupResults(loginToken(teacher), inProgress)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.final").value(false))
                .andExpect(jsonPath("$.groupGrade").value(1.5))
                .andExpect(jsonPath("$.members[0].individualGrade").value(1.5))
                .andExpect(jsonPath("$.submittedEvaluations").value(1))
                .andExpect(jsonPath("$.pendingEvaluations").value(1));
    }

    @Test
    void withoutSubmittedEvaluationsThereIsNoGradeYet() throws Exception {
        ProjectGroup ungraded = new ProjectGroup(closed, teacher, "Grupo sin enviar");
        ungraded.addMember(student("Dani", "Duarte"));
        evaluation(ungraded, teacher, level(closed, 0, 1), level(closed, 1, 1));
        ungraded = projectGroupRepository.saveAndFlush(ungraded);

        groupResults(loginToken(teacher), ungraded)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupGrade").value(nullValue()))
                .andExpect(jsonPath("$.groupLearningOutcomes[0].score").value(nullValue()))
                .andExpect(jsonPath("$.members[0].individualGrade").value(nullValue()))
                .andExpect(jsonPath("$.submittedEvaluations").value(0))
                .andExpect(jsonPath("$.pendingEvaluations").value(1));
    }

    @Test
    void onlyTheGroupTeacherTheFacultyAdminAndTheSuperAdminSeeTheGroupResults() throws Exception {
        groupResults(loginToken(admin(faculty)), graded).andExpect(status().isOk());
        groupResults(loginToken(persist(new SuperAdmin("Super", "Resultados", DocumentType.CC, document(),
                email("super"), passwordEncoder.encode(PASSWORD), now()))), graded)
                .andExpect(status().isOk());

        Faculty other = new Faculty("RO" + SEQUENCE.incrementAndGet(), "Otra facultad de resultados");
        facultyRepository.saveAndFlush(other);
        groupResults(loginToken(admin(other)), graded).andExpect(status().isForbidden());
        groupResults(loginToken(persist(new Teacher("Otro", "Docente", DocumentType.CC, document(),
                email("otro"), passwordEncoder.encode(PASSWORD), now(), faculty))), graded)
                .andExpect(status().isForbidden());
        groupResults(loginToken(business), graded).andExpect(status().isForbidden());
        groupResults(loginToken(ana), graded).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/groups/999999/results").header(HttpHeaders.AUTHORIZATION,
                        bearer(loginToken(teacher))))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ resultados del estudiante

    @Test
    void theStudentSeesTheirGradesAndCommentsOnlyAfterGradingCloses() throws Exception {
        ProjectGroup inProgress = new ProjectGroup(open, teacher, "Grupo en calificación");
        inProgress.addMember(ana);
        evaluation(inProgress, teacher, level(open, 0, 1), level(open, 1, 1)).submit();
        projectGroupRepository.saveAndFlush(inProgress);

        mockMvc.perform(get("/api/students/me/results").header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ana))))
                .andExpect(status().isOk())
                // El más reciente primero: mismo periodo, momento 2 antes que el 1.
                .andExpect(jsonPath("$[*].momentName", contains("Momento 2", "Momento 1")))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[0].finalGrade").value(nullValue()))
                .andExpect(jsonPath("$[0].groupGrade").value(nullValue()))
                .andExpect(jsonPath("$[0].learningOutcomes", empty()))
                .andExpect(jsonPath("$[0].comments", empty()))
                .andExpect(jsonPath("$[1].available").value(true))
                .andExpect(jsonPath("$[1].periodCode").value("2026-2"))
                .andExpect(jsonPath("$[1].groupName").value("Grupo calificado"))
                .andExpect(jsonPath("$[1].finalGrade").value(1.63))
                .andExpect(jsonPath("$[1].groupGrade").value(2.75))
                .andExpect(jsonPath("$[1].maxScore").value(3.0))
                .andExpect(jsonPath("$[1].learningOutcomes[*].score", contains(0.75, 0.88)))
                .andExpect(jsonPath("$[1].comments[*].evaluatorKind", contains("Docente", "Empresario")))
                .andExpect(jsonPath("$[1].comments[*].comment",
                        contains("Buen trabajo en equipo.", "Mejoren la presentación.")));
    }

    @Test
    void onlyStudentsSeeTheirOwnResults() throws Exception {
        mockMvc.perform(get("/api/students/me/results").header(HttpHeaders.AUTHORIZATION,
                        bearer(loginToken(teacher))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ datos de prueba

    /** Momento lanzado con dos RAP, los pesos 60/20/20 y la calificación hasta {@code gradingEnd}. */
    private static Moment launchedMoment(AcademicPeriod period, int number, LocalDate gradingEnd,
                                         EvaluatorType businessType, EvaluatorType alumnusType) {
        Moment moment = new Moment(period, number, "Momento " + number, "Asignatura " + number, 1, 3, "Bajo", 1, 5);
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 24));
        moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
        moment.setGradingStart(LocalDate.of(2026, 11, 2));
        moment.setGradingEnd(gradingEnd);
        moment.addLearningOutcome(outcome("RAP1", "0.00", "1.00"));
        moment.addLearningOutcome(outcome("RAP2", "0.50", "2.00"));
        moment.setTeacherWeight(new BigDecimal("60"));
        moment.setEvaluatorWeight(businessType, new BigDecimal("20"));
        moment.setEvaluatorWeight(alumnusType, new BigDecimal("20"));
        moment.launch();
        return moment;
    }

    /** RAP con un criterio y dos niveles: el bajo y el alto, con sus puntajes. */
    private static LearningOutcome outcome(String code, String low, String high) {
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio de " + code, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, new BigDecimal(low), 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(high), 2));
        LearningOutcome outcome = new LearningOutcome(code, "Descripción de " + code);
        outcome.addCriterion(criterion);
        return outcome;
    }

    /** Nivel {@code levelIndex} (0 bajo, 1 alto) del único criterio del RAP {@code outcomeIndex}. */
    private static PerformanceLevel level(Moment moment, int outcomeIndex, int levelIndex) {
        return moment.getLearningOutcomes().get(outcomeIndex).getCriteria().get(0).getLevels().get(levelIndex);
    }

    /** Evaluación en borrador del evaluador con un nivel por criterio. */
    private Evaluation evaluation(ProjectGroup group, User evaluator, PerformanceLevel... levels) {
        Evaluation evaluation = new Evaluation(evaluator, now());
        group.addEvaluation(evaluation);
        for (PerformanceLevel level : levels) {
            evaluation.grade(level);
        }
        return evaluation;
    }

    private ExternalEvaluator externalEvaluator(EvaluatorType type) {
        ExternalEvaluator evaluator = new ExternalEvaluator("Evaluador", "Resultados " + SEQUENCE.incrementAndGet(),
                DocumentType.CC, document(), email("evaluador"), passwordEncoder.encode(PASSWORD), now(), type);
        evaluator.setCreatedBy(teacher);
        return persist(evaluator);
    }

    private Student student(String firstNames, String lastNames) {
        String code = "R" + SEQUENCE.incrementAndGet();
        return persist(new Student(firstNames, lastNames, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), code, 2, program));
    }

    private Admin admin(Faculty adminFaculty) {
        return persist(new Admin("Admin", "Resultados", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), adminFaculty));
    }

    private <T extends User> T persist(T user) {
        return userRepository.saveAndFlush(user);
    }

    private ResultActions groupResults(String token, ProjectGroup group) throws Exception {
        return mockMvc.perform(get("/api/groups/" + group.getId() + "/results")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
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
        return "45" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".resultados" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
