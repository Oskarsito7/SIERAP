package co.edu.unicesar.sierap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import co.edu.unicesar.sierap.repository.GlobalParameterRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Pruebas de integración del módulo de reportes (Etapa 7, RF 12, docs/07 "Reporting").
 * Valida los 4 endpoints REST con PostgreSQL y el reloj fijo al 25 de noviembre de 2026:
 * <ul>
 *   <li>{@code GET /api/reports/individual}</li>
 *   <li>{@code GET /api/reports/groups/{id}}</li>
 *   <li>{@code GET /api/reports/moments/{id}}</li>
 *   <li>{@code GET /api/reports/comparative}</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ReportIntegrationTest {

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
    private GlobalParameterRepository globalParameterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicPeriod period1;
    private AcademicPeriod period2;
    private Faculty faculty;
    private Faculty otherFaculty;
    private Program program;
    private Moment closedMomentPeriod2;
    private Moment openMomentPeriod2;
    private Moment closedMomentPeriod1;

    private Teacher teacher;
    private Teacher otherTeacher;
    private Admin facultyAdmin;
    private Admin otherAdmin;
    private SuperAdmin superAdmin;
    private Student ana;
    private Student beto;
    private Student studentWithoutGroup;
    private ProjectGroup gradedGroup;

    /** Reloj fijo: 25 de noviembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 11, 25).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void setUp() {
        int n = SEQUENCE.incrementAndGet();

        period1 = academicPeriodRepository.findByCode("2026-1")
                .orElseGet(() -> academicPeriodRepository.saveAndFlush(
                        new AcademicPeriod("2026-1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30),
                                LocalDate.of(2026, 6, 25))));

        period2 = academicPeriodRepository.findByCode("2026-2").orElseThrow();

        EvaluatorType businessType = evaluatorTypeRepository.findByName("Empresario").orElseThrow();
        EvaluatorType alumnusType = evaluatorTypeRepository.findByName("Egresado").orElseThrow();

        faculty = new Faculty("RF" + n, "Facultad de Reportes " + n);
        otherFaculty = facultyRepository.saveAndFlush(new Faculty("ROF" + n, "Otra Facultad " + n));

        program = new Program("RP" + n, "Programa de Reportes " + n);
        faculty.addProgram(program);

        // Momentos en periodo 2:
        // closed: gradingEnd 20/11/2026 (anterior a today 25/11/2026 -> final = true)
        closedMomentPeriod2 = launchedMoment(period2, 1, LocalDate.of(2026, 11, 20), businessType, alumnusType);
        // open: gradingEnd 27/11/2026 (posterior a today -> final = false)
        openMomentPeriod2 = launchedMoment(period2, 2, LocalDate.of(2026, 11, 27), businessType, alumnusType);
        // Momento en periodo 1: number 1 (para comparativo)
        closedMomentPeriod1 = launchedMoment(period1, 1, LocalDate.of(2026, 5, 20), businessType, alumnusType);

        program.addMoment(closedMomentPeriod2);
        program.addMoment(openMomentPeriod2);
        program.addMoment(closedMomentPeriod1);
        faculty = facultyRepository.saveAndFlush(faculty);
        program = faculty.getPrograms().get(0);
        closedMomentPeriod2 = program.getMoments().get(0);
        openMomentPeriod2 = program.getMoments().get(1);
        closedMomentPeriod1 = program.getMoments().get(2);

        teacher = persist(new Teacher("Marta", "Díaz", DocumentType.CC, document(), email("marta"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        otherTeacher = persist(new Teacher("Pedro", "Pérez", DocumentType.CC, document(), email("pedro"),
                passwordEncoder.encode(PASSWORD), now(), otherFaculty));

        facultyAdmin = persist(new Admin("Admin", "Facultad", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        otherAdmin = persist(new Admin("Admin", "Otra", DocumentType.CC, document(), email("otheradmin"),
                passwordEncoder.encode(PASSWORD), now(), otherFaculty));
        superAdmin = persist(new SuperAdmin("Super", "Admin", DocumentType.CC, document(), email("super"),
                passwordEncoder.encode(PASSWORD), now()));

        ana = student("Ana", "Alba");
        beto = student("Beto", "Bravo");
        studentWithoutGroup = student("Carlos", "Castro");

        // Grupo calificado en periodo 2, momento 1
        gradedGroup = new ProjectGroup(closedMomentPeriod2, teacher, "Grupo 1");
        gradedGroup.setProjectTitle("Sistema SIERAP");
        gradedGroup.addMember(ana);
        gradedGroup.addMember(beto);

        ExternalEvaluator business = externalEvaluator(businessType);
        gradedGroup.addEvaluator(business);

        // Docente: RAP1=1.00, RAP2=2.00 (ajuste de Ana en RAP2 a 0.50)
        Evaluation evalTeacher = evaluation(gradedGroup, teacher, level(closedMomentPeriod2, 0, 1), level(closedMomentPeriod2, 1, 1));
        evalTeacher.adjust(ana, level(closedMomentPeriod2, 1, 0));
        evalTeacher.submit();

        // Empresario: RAP1=0.00, RAP2=2.00
        Evaluation evalBusiness = evaluation(gradedGroup, business, level(closedMomentPeriod2, 0, 0), level(closedMomentPeriod2, 1, 1));
        evalBusiness.submit();

        gradedGroup = projectGroupRepository.saveAndFlush(gradedGroup);

        // Grupo en periodo 1, momento 1 para comparativo
        ProjectGroup groupPeriod1 = new ProjectGroup(closedMomentPeriod1, teacher, "Grupo Antiguo");
        Student studentP1 = student("David", "Díaz");
        groupPeriod1.addMember(studentP1);
        Evaluation evalP1 = evaluation(groupPeriod1, teacher, level(closedMomentPeriod1, 0, 1), level(closedMomentPeriod1, 1, 1));
        evalP1.submit();
        projectGroupRepository.saveAndFlush(groupPeriod1);
    }

    // =========================================================================
    // 1. REPORTE INDIVIDUAL (GET /api/reports/individual)
    // =========================================================================
    @Nested
    @DisplayName("Reporte Individual")
    class IndividualReportTests {

        @Test
        @DisplayName("El estudiante puede consultar su propio reporte individual cuando la calificación cerró")
        void studentCanViewOwnIndividualReportWhenGradingClosed() throws Exception {
            String token = loginToken(ana);

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.student.fullName").value("Ana Alba"))
                    .andExpect(jsonPath("$.student.studentCode").value(ana.getStudentCode()))
                    .andExpect(jsonPath("$.group.name").value("Grupo 1"))
                    .andExpect(jsonPath("$.final").value(true))
                    // 0.75 * 1.50 + 0.25 * 2.00 = 1.625 -> 1.63
                    .andExpect(jsonPath("$.individualGrade").value(1.63))
                    .andExpect(jsonPath("$.groupGrade").value(2.75))
                    .andExpect(jsonPath("$.percentage").value(54.33))
                    .andExpect(jsonPath("$.learningOutcomes", hasSize(2)))
                    .andExpect(jsonPath("$.learningOutcomes[0].code").value("RAP1"))
                    .andExpect(jsonPath("$.learningOutcomes[0].score").value(0.75))
                    .andExpect(jsonPath("$.learningOutcomes[1].code").value("RAP2"))
                    .andExpect(jsonPath("$.learningOutcomes[1].score").value(0.88))
                    .andExpect(jsonPath("$.submittedEvaluations").value(2))
                    .andExpect(jsonPath("$.pendingEvaluations").value(0));
        }

        @Test
        @DisplayName("El estudiante no puede consultar el reporte de otro estudiante")
        void studentCannotViewOtherStudentReport() throws Exception {
            String token = loginToken(ana);

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", beto.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Solo puede consultar su propio reporte individual."));
        }

        @Test
        @DisplayName("El estudiante recibe 422 si la ventana de calificación sigue abierta")
        void studentReceives422WhileGradingIsOpen() throws Exception {
            // Asignar estudiante a un grupo en el momento abierto
            ProjectGroup inProgress = new ProjectGroup(openMomentPeriod2, teacher, "Grupo abierto");
            inProgress.addMember(ana);
            projectGroupRepository.saveAndFlush(inProgress);

            String token = loginToken(ana);

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", openMomentPeriod2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Los resultados de este momento aún no están disponibles."));
        }

        @Test
        @DisplayName("El docente del grupo puede consultar el reporte individual del estudiante con vista previa")
        void teacherOfGroupCanViewStudentReport() throws Exception {
            String token = loginToken(teacher);

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", ana.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.student.fullName").value("Ana Alba"))
                    .andExpect(jsonPath("$.individualGrade").value(1.63));
        }

        @Test
        @DisplayName("Un docente que no habilitó el grupo recibe 403")
        void otherTeacherCannotViewStudentReport() throws Exception {
            Teacher facultyTeacherWithoutGroup = persist(new Teacher("Lucas", "López", DocumentType.CC, document(),
                    email("lucas"), passwordEncoder.encode(PASSWORD), now(), faculty));
            String token = loginToken(facultyTeacherWithoutGroup);

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", ana.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("El admin de facultad y el super admin pueden consultar el reporte individual")
        void adminAndSuperAdminCanViewIndividualReport() throws Exception {
            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", ana.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(facultyAdmin))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.student.fullName").value("Ana Alba"));

            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", ana.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin de otra facultad recibe 403 al consultar reporte individual")
        void otherAdminCannotViewIndividualReport() throws Exception {
            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .param("studentId", ana.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(otherAdmin))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Estudiante que no pertenece a ningún grupo en el momento recibe 422")
        void studentNotInAnyGroupReceives422() throws Exception {
            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", closedMomentPeriod2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(studentWithoutGroup))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("El estudiante no pertenece a ningún grupo de este momento."));
        }

        @Test
        @DisplayName("Momento inexistente responde 404")
        void nonExistentMomentReturns404() throws Exception {
            mockMvc.perform(get("/api/reports/individual")
                            .param("momentId", "999999")
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // 2. REPORTE POR GRUPO (GET /api/reports/groups/{id})
    // =========================================================================
    @Nested
    @DisplayName("Reporte por Grupo")
    class GroupReportTests {

        @Test
        @DisplayName("El docente del grupo puede consultar el reporte por grupo con notas y ajustes")
        void teacherCanViewGroupReport() throws Exception {
            String token = loginToken(teacher);

            mockMvc.perform(get("/api/reports/groups/" + gradedGroup.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Grupo 1"))
                    .andExpect(jsonPath("$.groupGrade").value(2.75))
                    .andExpect(jsonPath("$.percentage").value(91.67))
                    .andExpect(jsonPath("$.members", hasSize(2)))
                    .andExpect(jsonPath("$.members[*].fullName", contains("Ana Alba", "Beto Bravo")))
                    .andExpect(jsonPath("$.members[0].individualGrade").value(1.63))
                    .andExpect(jsonPath("$.members[0].adjusted").value(true))
                    .andExpect(jsonPath("$.members[1].individualGrade").value(2.75))
                    .andExpect(jsonPath("$.members[1].adjusted").value(false));
        }

        @Test
        @DisplayName("Admin de facultad y Super Admin pueden consultar el reporte por grupo")
        void adminAndSuperAdminCanViewGroupReport() throws Exception {
            mockMvc.perform(get("/api/reports/groups/" + gradedGroup.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(facultyAdmin))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.group.name").value("Grupo 1"));

            mockMvc.perform(get("/api/reports/groups/" + gradedGroup.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Docente de otra facultad o que no habilitó el grupo recibe 403")
        void unauthorizedTeacherCannotViewGroupReport() throws Exception {
            mockMvc.perform(get("/api/reports/groups/" + gradedGroup.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(otherTeacher))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un estudiante recibe 403 al intentar acceder al reporte de grupo")
        void studentCannotViewGroupReport() throws Exception {
            mockMvc.perform(get("/api/reports/groups/" + gradedGroup.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ana))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Grupo inexistente responde 404")
        void nonExistentGroupReturns404() throws Exception {
            mockMvc.perform(get("/api/reports/groups/999999")
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // 3. REPORTE CONSOLIDADO POR MOMENTO (GET /api/reports/moments/{id})
    // =========================================================================
    @Nested
    @DisplayName("Reporte Consolidado por Momento")
    class MomentReportTests {

        @Test
        @DisplayName("Admin de facultad puede consultar consolidado por momento con distribución y promedios")
        void adminCanViewMomentReport() throws Exception {
            String token = loginToken(facultyAdmin);

            mockMvc.perform(get("/api/reports/moments/" + closedMomentPeriod2.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.program.code").value(program.getCode()))
                    .andExpect(jsonPath("$.moment.momentNumber").value(1))
                    .andExpect(jsonPath("$.totalGroups").value(1))
                    .andExpect(jsonPath("$.gradedGroups").value(1))
                    .andExpect(jsonPath("$.totalStudents").value(2))
                    .andExpect(jsonPath("$.gradedStudents").value(2))
                    .andExpect(jsonPath("$.averageGroupGrade").value(2.75))
                    // Promedio de estudiantes: (1.63 + 2.75) / 2 = 2.19
                    .andExpect(jsonPath("$.averageGrade").value(2.19))
                    .andExpect(jsonPath("$.minGrade").value(1.63))
                    .andExpect(jsonPath("$.maxGrade").value(2.75))
                    .andExpect(jsonPath("$.passingCount").value(1))
                    .andExpect(jsonPath("$.passingPercentage").value(50.0))
                    .andExpect(jsonPath("$.distribution", hasSize(4)))
                    .andExpect(jsonPath("$.distribution[0].label").value("Bajo"))
                    .andExpect(jsonPath("$.distribution[0].range").value("0.00 - 1.79"))
                    .andExpect(jsonPath("$.distribution[0].count").value(1))
                    .andExpect(jsonPath("$.distribution[0].percentage").value(50.0))
                    .andExpect(jsonPath("$.distribution[3].label").value("Sobresaliente"))
                    .andExpect(jsonPath("$.distribution[3].range").value("2.70 - 3.00"))
                    .andExpect(jsonPath("$.distribution[3].count").value(1))
                    .andExpect(jsonPath("$.distribution[3].percentage").value(50.0))
                    .andExpect(jsonPath("$.learningOutcomes", hasSize(2)));
        }

        @Test
        @DisplayName("Docente de la facultad puede consultar el consolidado del momento")
        void facultyTeacherCanViewMomentReport() throws Exception {
            mockMvc.perform(get("/api/reports/moments/" + closedMomentPeriod2.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(teacher))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGroups").value(1));
        }

        @Test
        @DisplayName("Docente de otra facultad recibe 403 al consultar consolidado")
        void otherFacultyTeacherCannotViewMomentReport() throws Exception {
            mockMvc.perform(get("/api/reports/moments/" + closedMomentPeriod2.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(otherTeacher))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Estudiante recibe 403 al consultar reporte consolidado de momento")
        void studentCannotViewMomentReport() throws Exception {
            mockMvc.perform(get("/api/reports/moments/" + closedMomentPeriod2.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ana))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Si falta un parámetro global de reporte, responde 422 con mensaje claro")
        void missingGlobalParameterReturns422() throws Exception {
            globalParameterRepository.deleteAll();

            mockMvc.perform(get("/api/reports/moments/" + closedMomentPeriod2.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(facultyAdmin))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("Falta configurar el parámetro global")));
        }
    }

    // =========================================================================
    // 4. REPORTE COMPARATIVO (GET /api/reports/comparative)
    // =========================================================================
    @Nested
    @DisplayName("Reporte Comparativo Semestre X vs. Y")
    class ComparativeReportTests {

        @Test
        @DisplayName("Admin puede consultar comparativo entre dos semestres con deltas y datos exclusivamente agregados")
        void adminCanViewComparativeReport() throws Exception {
            String token = loginToken(facultyAdmin);

            ResultActions result = mockMvc.perform(get("/api/reports/comparative")
                            .param("programId", program.getId().toString())
                            .param("momentNumber", "1")
                            .param("periodAId", period1.getId().toString())
                            .param("periodBId", period2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.program.code").value(program.getCode()))
                    .andExpect(jsonPath("$.momentNumber").value(1))
                    .andExpect(jsonPath("$.periodA.period.code").value("2026-1"))
                    .andExpect(jsonPath("$.periodB.period.code").value("2026-2"))
                    .andExpect(jsonPath("$.periodA.averageGrade").value(3.0))
                    .andExpect(jsonPath("$.periodB.averageGrade").value(2.19))
                    // Delta: 2.19 - 3.00 = -0.81
                    .andExpect(jsonPath("$.deltaAverageGrade").value(-0.81))
                    .andExpect(jsonPath("$.periodA.passingPercentage").value(100.0))
                    .andExpect(jsonPath("$.periodB.passingPercentage").value(50.0))
                    .andExpect(jsonPath("$.deltaPassingPercentage").value(-50.0))
                    .andExpect(jsonPath("$.learningOutcomes", hasSize(2)))
                    .andExpect(jsonPath("$.learningOutcomes[0].code").value("RAP1"))
                    .andExpect(jsonPath("$.learningOutcomes[1].code").value("RAP2"));

            // Validar que el comparativo es exclusivamente agregado: no contiene listas de estudiantes ni nombres
            String responseBody = result.andReturn().getResponse().getContentAsString();
            assertThat(responseBody).doesNotContain("studentCode");
            assertThat(responseBody).doesNotContain("Ana Alba");
            assertThat(responseBody).doesNotContain("Beto Bravo");
        }

        @Test
        @DisplayName("Elegir el mismo periodo para A y B responde 422")
        void samePeriodReturns422() throws Exception {
            mockMvc.perform(get("/api/reports/comparative")
                            .param("programId", program.getId().toString())
                            .param("momentNumber", "1")
                            .param("periodAId", period1.getId().toString())
                            .param("periodBId", period1.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(facultyAdmin))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Debe elegir dos periodos académicos distintos para comparar."));
        }

        @Test
        @DisplayName("Estudiante recibe 403 al intentar consultar reporte comparativo")
        void studentCannotViewComparativeReport() throws Exception {
            mockMvc.perform(get("/api/reports/comparative")
                            .param("programId", program.getId().toString())
                            .param("momentNumber", "1")
                            .param("periodAId", period1.getId().toString())
                            .param("periodBId", period2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ana))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Programa sin el momento especificado en uno de los periodos responde 404")
        void missingMomentNumberInPeriodReturns404() throws Exception {
            mockMvc.perform(get("/api/reports/comparative")
                            .param("programId", program.getId().toString())
                            .param("momentNumber", "99")
                            .param("periodAId", period1.getId().toString())
                            .param("periodBId", period2.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(facultyAdmin))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Moment launchedMoment(AcademicPeriod period, int number, LocalDate gradingEnd,
                                         EvaluatorType businessType, EvaluatorType alumnusType) {
        Moment moment = new Moment(period, number, "Momento " + number, "Asignatura " + number, 1, 3, "Bajo", 1, 5);
        if (period.getCode().contains("2026-1")) {
            moment.setGroupFormationStart(LocalDate.of(2026, 2, 10));
            moment.setGroupFormationEnd(LocalDate.of(2026, 2, 24));
            moment.setSubmissionStart(LocalDate.of(2026, 3, 1));
            moment.setSubmissionEnd(LocalDate.of(2026, 4, 30));
            moment.setGradingStart(LocalDate.of(2026, 5, 2));
            moment.setGradingEnd(gradingEnd);
        } else {
            moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
            moment.setGroupFormationEnd(LocalDate.of(2026, 8, 24));
            moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
            moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
            moment.setGradingStart(LocalDate.of(2026, 11, 2));
            moment.setGradingEnd(gradingEnd);
        }
        moment.addLearningOutcome(outcome("RAP1", "0.00", "1.00"));
        moment.addLearningOutcome(outcome("RAP2", "0.50", "2.00"));
        moment.setTeacherWeight(new BigDecimal("60"));
        moment.setEvaluatorWeight(businessType, new BigDecimal("20"));
        moment.setEvaluatorWeight(alumnusType, new BigDecimal("20"));
        moment.launch();
        return moment;
    }

    private static LearningOutcome outcome(String code, String low, String high) {
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio de " + code, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, new BigDecimal(low), 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(high), 2));
        LearningOutcome outcome = new LearningOutcome(code, "Descripción de " + code);
        outcome.addCriterion(criterion);
        return outcome;
    }

    private static PerformanceLevel level(Moment moment, int outcomeIndex, int levelIndex) {
        return moment.getLearningOutcomes().get(outcomeIndex).getCriteria().get(0).getLevels().get(levelIndex);
    }

    private Evaluation evaluation(ProjectGroup group, User evaluator, PerformanceLevel... levels) {
        Evaluation eval = new Evaluation(evaluator, now());
        group.addEvaluation(eval);
        for (PerformanceLevel level : levels) {
            eval.grade(level);
        }
        return eval;
    }

    private ExternalEvaluator externalEvaluator(EvaluatorType type) {
        ExternalEvaluator evaluator = new ExternalEvaluator("Evaluador", "Externo " + SEQUENCE.incrementAndGet(),
                DocumentType.CC, document(), email("evaluador"), passwordEncoder.encode(PASSWORD), now(), type);
        evaluator.setCreatedBy(teacher);
        return persist(evaluator);
    }

    private Student student(String firstNames, String lastNames) {
        String code = "R" + SEQUENCE.incrementAndGet();
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
        return "45" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".reportes" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
