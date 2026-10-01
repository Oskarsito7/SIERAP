package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de las entregas (Etapa 5, {@code docs/07} "Submissions") contra
 * PostgreSQL real, con los archivos en una carpeta temporal y un reloj fijo.
 *
 * <p>Hoy es el 28 de septiembre de 2026. Cada prueba crea una facultad con un programa y
 * tres momentos, y se revierte al terminar (los archivos guardados se borran al
 * revertirse la transacción):</p>
 * <ul>
 *   <li>{@code launched}: lanzado, ventana de entregas abierta (1 de septiembre al 31 de
 *       octubre). Su grupo tiene dos integrantes y un evaluador externo.</li>
 *   <li>{@code closed}: lanzado, la ventana de entregas cerró el 10 de agosto.</li>
 *   <li>{@code configuring}: todavía en configuración.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class SubmissionIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final byte[] ZIP = bytes("PK\u0003\u0004 proyecto");
    private static final byte[] PDF = bytes("%PDF-1.7 sustentación");

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
    private Moment launched;
    private Moment closed;
    private Moment configuring;
    private Teacher teacher;
    private ExternalEvaluator evaluator;
    private ProjectGroup group;
    private String memberToken;

    /** Reloj fijo: 28 de septiembre de 2026 en Bogotá. */
    static Clock clock() {
        return Clock.fixed(LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota"));
    }

    @BeforeEach
    void createMomentsAndGroup() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();

        faculty = new Faculty("F" + n, "Facultad de entregas " + n);
        program = new Program("FP" + n, "Programa de entregas " + n);
        faculty.addProgram(program);

        launched = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 2, 4);
        setWindows(launched, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));
        launched.addLearningOutcome(outcome());
        launched.launch();
        program.addMoment(launched);

        closed = new Moment(period, 2, "Momento 2", "Asignatura 2", 1, 3, "Bajo", 2, 4);
        setWindows(closed, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 10));
        closed.addLearningOutcome(outcome());
        closed.launch();
        program.addMoment(closed);

        configuring = new Moment(period, 3, "Momento 3", "Asignatura 3", 1, 3, "Bajo", 2, 4);
        setWindows(configuring, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        configuring.addLearningOutcome(outcome());
        program.addMoment(configuring);
        facultyRepository.saveAndFlush(faculty);

        teacher = persist(new Teacher("Docente", "Prueba", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        evaluator = new ExternalEvaluator("Evaluador", "Prueba", DocumentType.CC, document(),
                "evaluador.entregas" + SEQUENCE.incrementAndGet() + "@correo.com",
                passwordEncoder.encode(PASSWORD), now(),
                evaluatorTypeRepository.findByName("Empresario").orElseThrow());
        evaluator.setCreatedBy(teacher);
        evaluator = persist(evaluator);

        Student member = student();
        group = new ProjectGroup(launched, teacher, "Grupo 1");
        group.addMember(member);
        group.addMember(student());
        group.addEvaluator(evaluator);
        group = projectGroupRepository.saveAndFlush(group);
        memberToken = loginToken(member);
    }

    // ------------------------------------------------------------ subir versiones

    @Test
    void eachUploadIsANewVersionAndTheHistoryShowsTheCurrentPresentation() throws Exception {
        upload(memberToken, group, zip("proyecto-v1.zip"), pdf("sustentacion.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.late").value(false))
                .andExpect(jsonPath("$.zipFileName").value("proyecto-v1.zip"))
                .andExpect(jsonPath("$.zipSize").value(ZIP.length))
                .andExpect(jsonPath("$.presentationFileName").value("sustentacion.pdf"))
                .andExpect(jsonPath("$.submittedAt").value("2026-09-28T00:00:00"))
                .andExpect(jsonPath("$.toGrade").value(true));
        upload(memberToken, group, zip("proyecto-v2.zip"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.presentationFileName").value(nullValue()));

        mockMvc.perform(get("/api/groups/" + group.getId() + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[*].versionNumber", contains(2, 1)))
                .andExpect(jsonPath("$.versions[*].toGrade", contains(true, false)))
                .andExpect(jsonPath("$.currentPresentation.versionNumber").value(1));
    }

    @Test
    void uploadsAfterTheGroupDeadlineAreLateAndTheLastOnTimeVersionIsGraded() throws Exception {
        upload(memberToken, group, zip("a-tiempo.zip"), null).andExpect(jsonPath("$.late").value(false));
        group.setSubmissionDeadline(LocalDate.of(2026, 9, 20));
        projectGroupRepository.saveAndFlush(group);

        upload(memberToken, group, zip("tarde.zip"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.late").value(true))
                .andExpect(jsonPath("$.toGrade").value(false));
        mockMvc.perform(get("/api/groups/" + group.getId() + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(jsonPath("$.versions[*].late", contains(true, false)))
                .andExpect(jsonPath("$.versions[*].toGrade", contains(false, true)))
                .andExpect(jsonPath("$.currentPresentation").value(nullValue()));
    }

    // ------------------------------------------------------------ archivos inválidos

    @Test
    void filesOfTheWrongTypeAreRejected() throws Exception {
        upload(memberToken, group, null, pdf("sustentacion.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Adjunte el archivo .zip con el proyecto."));
        upload(memberToken, group, new MockMultipartFile("zip", "proyecto.rar", "application/zip", ZIP), null)
                .andExpect(status().isBadRequest());
        upload(memberToken, group, new MockMultipartFile("zip", "proyecto.zip", "application/zip", PDF), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El proyecto debe ser un archivo .zip válido."));
        upload(memberToken, group, zip("proyecto.zip"),
                new MockMultipartFile("presentation", "virus.exe", "application/octet-stream", ZIP))
                .andExpect(status().isBadRequest());
        upload(memberToken, group, zip("proyecto.zip"),
                new MockMultipartFile("presentation", "sustentacion.pdf", "text/plain", PDF))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPresentationAboveThirtyMegabytesIsTooLarge() throws Exception {
        byte[] large = Arrays.copyOf(PDF, 30 * 1024 * 1024 + 1);
        upload(memberToken, group, zip("proyecto.zip"),
                new MockMultipartFile("presentation", "sustentacion.pdf", "application/pdf", large))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("La presentación supera el máximo de 30 MB."));
        mockMvc.perform(get("/api/groups/" + group.getId() + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(jsonPath("$.versions").isEmpty());
    }

    // ------------------------------------------------------------ quién y cuándo

    @Test
    void onlyMembersUploadAndOnlyWhileTheSubmissionWindowIsOpen() throws Exception {
        upload(loginToken(student()), group, zip("ajeno.zip"), null).andExpect(status().isForbidden());
        upload(loginToken(teacher), group, zip("docente.zip"), null).andExpect(status().isForbidden());

        Student member = student();
        ProjectGroup closedGroup = new ProjectGroup(closed, teacher, "Grupo cerrado");
        closedGroup.addMember(member);
        ProjectGroup configuringGroup = new ProjectGroup(configuring, teacher, "Grupo en configuración");
        configuringGroup.addMember(member);
        projectGroupRepository.saveAndFlush(closedGroup);
        projectGroupRepository.saveAndFlush(configuringGroup);
        String token = loginToken(member);

        upload(token, closedGroup, zip("tarde.zip"), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La ventana de entregas cerró el 2026-08-10."));
        upload(token, configuringGroup, zip("pronto.zip"), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("todavía no se ha lanzado")));
    }

    // ------------------------------------------------------------ consultar y descargar

    @Test
    void theGroupItsTeacherAndItsEvaluatorsDownloadTheFiles() throws Exception {
        long id = submissionId(upload(memberToken, group, zip("Proyecto final.zip"), pdf("sustentación.pdf")));
        long withoutPresentation = submissionId(upload(memberToken, group, zip("v2.zip"), null));

        for (String token : new String[] {memberToken, loginToken(teacher), loginToken(evaluator)}) {
            mockMvc.perform(get("/api/submissions/" + id + "/zip").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(ZIP))
                    .andExpect(content().contentType("application/zip"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("filename*=UTF-8''Proyecto%20final.zip")));
        }
        mockMvc.perform(get("/api/submissions/" + id + "/presentation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(teacher))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mockMvc.perform(get("/api/submissions/" + withoutPresentation + "/presentation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void outsidersCannotSeeOrDownloadTheSubmissions() throws Exception {
        long id = submissionId(upload(memberToken, group, zip("proyecto.zip"), null));
        Faculty otherFaculty = facultyRepository.saveAndFlush(new Faculty("X" + SEQUENCE.incrementAndGet(),
                "Facultad ajena " + SEQUENCE.get()));
        Admin foreignAdmin = persist(new Admin("Admin", "Ajeno", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), otherFaculty));
        Admin ownAdmin = persist(new Admin("Admin", "Propio", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        Teacher otherTeacher = persist(new Teacher("Docente", "Ajeno", DocumentType.CC, document(),
                email("docente"), passwordEncoder.encode(PASSWORD), now(), faculty));

        for (User outsider : new User[] {student(), otherTeacher, foreignAdmin}) {
            String token = loginToken(outsider);
            mockMvc.perform(get("/api/groups/" + group.getId() + "/submissions")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/submissions/" + id + "/zip").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/groups/" + group.getId() + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(ownAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[*].versionNumber", contains(1)));
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions upload(String token, ProjectGroup target, MockMultipartFile zip,
                                 MockMultipartFile presentation) throws Exception {
        var request = multipart("/api/groups/" + target.getId() + "/submissions");
        if (zip != null) {
            request.file(zip);
        }
        if (presentation != null) {
            request.file(presentation);
        }
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static long submissionId(ResultActions upload) throws Exception {
        String body = upload.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static MockMultipartFile zip(String name) {
        return new MockMultipartFile("zip", name, "application/zip", ZIP);
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("presentation", name, "application/pdf", PDF);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
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

    private Student student() {
        String code = "F" + SEQUENCE.incrementAndGet();
        return persist(new Student("Estudiante", "Prueba " + code, DocumentType.TI, document(), email("estudiante"),
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
        return "35" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".entregas" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
