package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.RegistrationCode;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
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
 * Prueba de integración de "Mis estudiantes" (DO-08, {@code docs/07} "Stage 2 —
 * Accounts") contra PostgreSQL real: los datos de cada estudiante, cómo llegó, la marca
 * de los que gestiona quien consulta y el filtro "Sin grupo".
 *
 * <p>Cada prueba crea una facultad con dos programas y se revierte al terminar. En el
 * programa hay tres estudiantes, ordenados por apellido: Alba (la creó el docente),
 * Bravo (se registró con el código del docente) y Cruz (la creó otro docente).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class StudentAccountIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private RegistrationCodeRepository registrationCodeRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    private Faculty faculty;
    private Program program;
    private Moment moment;
    private Moment otherProgramMoment;
    private Teacher teacher;
    private Teacher otherTeacher;
    private RegistrationCode code;
    private Student alba;
    private Student bravo;
    private Student cruz;

    @BeforeEach
    void createFacultyTeachersAndStudents() {
        int n = SEQUENCE.incrementAndGet();
        AcademicPeriod period = academicPeriodRepository.findByCode("2026-2").orElseThrow();

        faculty = new Faculty("E" + n, "Facultad de estudiantes " + n);
        program = new Program("EP" + n, "Programa de estudiantes " + n);
        Program otherProgram = new Program("EO" + n, "Otro programa " + n);
        faculty.addProgram(program);
        faculty.addProgram(otherProgram);
        moment = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 2, 4);
        program.addMoment(moment);
        otherProgramMoment = new Moment(period, 1, "Momento 1", "Asignatura 1", 1, 3, "Bajo", 2, 4);
        otherProgram.addMoment(otherProgramMoment);
        facultyRepository.saveAndFlush(faculty);

        teacher = persist(new Teacher("Docente", "Dueño " + n, DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        otherTeacher = persist(new Teacher("Otro", "Docente " + n, DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        code = registrationCodeRepository.saveAndFlush(new RegistrationCode("EST" + n, teacher, program,
                LocalDate.now(clock).plusDays(10), now()));

        alba = student("Alba", 2, teacher, null);
        bravo = student("Bravo", 1, teacher, code);
        cruz = student("Cruz", 3, otherTeacher, null);
    }

    @Test
    void theTeacherSeesEveryStudentOfTheProgramAndWhichOnesTheyManage() throws Exception {
        String token = loginToken(teacher);

        list(token, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].lastNames", contains("Alba", "Bravo", "Cruz")))
                .andExpect(jsonPath("$[0].studentCode").value(alba.getStudentCode()))
                .andExpect(jsonPath("$[0].currentSemester").value(2))
                .andExpect(jsonPath("$[0].registrationCode").value(nullValue()))
                .andExpect(jsonPath("$[0].createdBy.id").value(teacher.getId()))
                .andExpect(jsonPath("$[0].manageable").value(true))
                .andExpect(jsonPath("$[1].registrationCode").value(code.getCode()))
                .andExpect(jsonPath("$[1].createdBy.id").value(teacher.getId()))
                .andExpect(jsonPath("$[1].manageable").value(true))
                .andExpect(jsonPath("$[2].createdBy.fullName").value(otherTeacher.getFullName()))
                .andExpect(jsonPath("$[2].manageable").value(false));

        // La marca coincide con la regla que el backend aplica en cada operación.
        mockMvc.perform(patch("/api/users/" + cruz.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAdminOfTheFacultyManagesEveryStudentAndOtherFacultiesSeeNone() throws Exception {
        Admin admin = persist(new Admin("Admin", "Estudiantes", DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), faculty));

        list(loginToken(admin), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].manageable", contains(true, true, true)));

        Faculty otherFaculty = facultyRepository.saveAndFlush(
                new Faculty("F" + SEQUENCE.incrementAndGet(), "Facultad ajena"));
        Teacher outsider = persist(new Teacher("Docente", "Ajeno", DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), otherFaculty));
        list(loginToken(outsider), Map.of()).andExpect(status().isForbidden());
    }

    @Test
    void aCreatedStudentComesBackManageableByItsCreator() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Map<String, Object> body = Map.of("firstNames", "Nueva", "lastNames", "Estudiante", "documentType", "TI",
                "documentNumber", document(), "email", email("nueva"), "programId", program.getId(),
                "studentCode", "N" + n, "currentSemester", 4);

        mockMvc.perform(post("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(teacher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentCode").value("N" + n))
                .andExpect(jsonPath("$.currentSemester").value(4))
                .andExpect(jsonPath("$.registrationCode").value(nullValue()))
                .andExpect(jsonPath("$.createdBy.id").value(teacher.getId()))
                .andExpect(jsonPath("$.manageable").value(true));
    }

    @Test
    void withoutGroupInMomentKeepsTheRuleOfTheGroupScreen() throws Exception {
        ProjectGroup group = new ProjectGroup(moment, teacher, "Grupo 1");
        group.addMember(bravo);
        projectGroupRepository.saveAndFlush(group);
        student("Duarte", 5, teacher, null);
        Student inactive = student("Esquivel", 2, teacher, null);
        inactive.setActive(false);
        userRepository.saveAndFlush(inactive);
        String token = loginToken(teacher);

        // Sin Bravo (ya tiene grupo), Duarte (semestre 5, fuera de 1 a 3) ni Esquivel (inactiva).
        list(token, Map.of("withoutGroupInMoment", moment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].lastNames", contains("Alba", "Cruz")))
                .andExpect(jsonPath("$[1].manageable").value(false));

        list(token, Map.of("withoutGroupInMoment", otherProgramMoment.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "El momento Momento 1 no es del programa " + program.getName() + "."));
        list(token, Map.of("withoutGroupInMoment", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions list(String token, Map<String, Object> extraParams) throws Exception {
        var request = get("/api/students")
                .param("programId", program.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        extraParams.forEach((name, value) -> request.param(name, value.toString()));
        return mockMvc.perform(request);
    }

    private Student student(String lastNames, int semester, Teacher creator, RegistrationCode usedCode) {
        Student student = new Student("Estudiante", lastNames, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), "C" + SEQUENCE.incrementAndGet(), semester, program);
        student.setCreatedBy(creator);
        student.setRegistrationCode(usedCode);
        return persist(student);
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
        return "44" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".estudiantes" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
