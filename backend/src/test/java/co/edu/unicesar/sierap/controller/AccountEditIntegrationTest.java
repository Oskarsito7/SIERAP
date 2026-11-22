package co.edu.unicesar.sierap.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.RefreshCookie;
import co.edu.unicesar.sierap.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la edición de cuentas ({@code PATCH /api/users/{id}}) y del
 * semestre en lote ({@code PATCH /api/students/semester}), contra PostgreSQL real.
 *
 * <p>Cada prueba crea una facultad con un programa, un admin, dos docentes y estudiantes
 * de ambos docentes, y se revierte al terminar.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountEditIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    private Faculty faculty;
    private Program program;
    private Admin admin;
    private Teacher teacher;
    private Teacher otherTeacher;
    private Student own;
    private Student ownToo;
    private Student foreign;

    @BeforeEach
    void createScenario() {
        int n = SEQUENCE.incrementAndGet();
        faculty = new Faculty("ED" + n, "Facultad de edición " + n);
        program = new Program("PE" + n, "Programa de edición " + n);
        faculty.addProgram(program);
        facultyRepository.saveAndFlush(faculty);

        admin = persist(new Admin("Admin", "Edición " + n, DocumentType.CC, document(), email("admin"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        teacher = persist(new Teacher("Docente", "Uno " + n, DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        otherTeacher = persist(new Teacher("Docente", "Dos " + n, DocumentType.CC, document(), email("docente"),
                passwordEncoder.encode(PASSWORD), now(), faculty));
        own = student("Propio", teacher);
        ownToo = student("Propia", teacher);
        foreign = student("Ajeno", otherTeacher);
    }

    // ------------------------------------------------------------ privacidad del documento

    @Test
    void theDocumentIsOnlyShownToWhoManagesTheAccount() throws Exception {
        mockMvc.perform(get("/api/students").param("programId", program.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + own.getId() + ")].documentNumber")
                        .value(own.getDocumentNumber()))
                .andExpect(jsonPath("$[?(@.id == " + foreign.getId() + ")].manageable").value(false))
                .andExpect(jsonPath("$[?(@.id == " + foreign.getId() + ")].documentNumber").value(nullValue()));
    }

    // ------------------------------------------------------------ edición

    @Test
    void editsThePersonalDataOfAStudentAndNormalizesTheEmail() throws Exception {
        Map<String, Object> body = Map.of("firstNames", "  Nuevo Nombre ", "lastNames", "Nuevo Apellido",
                "documentType", "CE", "documentNumber", "99" + SEQUENCE.incrementAndGet(),
                "email", "  NUEVO.Correo" + SEQUENCE.incrementAndGet() + "@UniCesar.edu.co ",
                "studentCode", "NC" + SEQUENCE.incrementAndGet(), "currentSemester", 7);

        patchUser(loginToken(teacher), own.getId(), body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstNames").value("Nuevo Nombre"))
                .andExpect(jsonPath("$.lastNames").value("Nuevo Apellido"))
                .andExpect(jsonPath("$.email").value(((String) body.get("email")).trim().toLowerCase()));

        userRepository.flush();
        Student saved = studentRepository.findById(own.getId()).orElseThrow();
        assertThat(saved.getDocumentType()).isEqualTo(DocumentType.CE);
        assertThat(saved.getDocumentNumber()).isEqualTo(body.get("documentNumber"));
        assertThat(saved.getStudentCode()).isEqualTo(body.get("studentCode"));
        assertThat(saved.getCurrentSemester()).isEqualTo(7);
    }

    @Test
    void aPartialBodyOnlyChangesTheSentFieldsAndKeepingTheOwnValuesIsNotADuplicate() throws Exception {
        patchUser(loginToken(admin), own.getId(),
                Map.of("lastNames", "Apellido corregido", "email", own.getEmail().toUpperCase(),
                        "studentCode", own.getStudentCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastNames").value("Apellido corregido"))
                .andExpect(jsonPath("$.firstNames").value(own.getFirstNames()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void anEmptyBodyIs422() throws Exception {
        patchUser(loginToken(teacher), own.getId(), Map.of())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No hay cambios para guardar."));
    }

    @Test
    void invalidFieldsAreRejected() throws Exception {
        String token = loginToken(teacher);

        patchUser(token, own.getId(), Map.of("email", "no-es-correo")).andExpect(status().isBadRequest());
        patchUser(token, own.getId(), Map.of("firstNames", "   ")).andExpect(status().isBadRequest());
        patchUser(token, own.getId(), Map.of("currentSemester", 0)).andExpect(status().isBadRequest());
    }

    @Test
    void aRepeatedEmailDocumentOrCodeIs422WithTheCreationMessages() throws Exception {
        String token = loginToken(teacher);

        patchUser(token, own.getId(), Map.of("email", ownToo.getEmail().toUpperCase()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con el correo " + ownToo.getEmail() + "."));
        patchUser(token, own.getId(), Map.of("documentNumber", ownToo.getDocumentNumber()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Ya existe una cuenta con el documento " + ownToo.getDocumentNumber() + "."));
        patchUser(token, own.getId(), Map.of("studentCode", ownToo.getStudentCode()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un estudiante con el código " + ownToo.getStudentCode() + "."));
    }

    @Test
    void anAccountOutOfScopeIs403() throws Exception {
        patchUser(loginToken(teacher), foreign.getId(), Map.of("firstNames", "Intruso"))
                .andExpect(status().isForbidden());
        patchUser(loginToken(teacher), otherTeacher.getId(), Map.of("firstNames", "Intruso"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentFieldsOnATeacherAreRejected() throws Exception {
        String token = loginToken(admin);

        patchUser(token, teacher.getId(), Map.of("studentCode", "X1"))
                .andExpect(status().isUnprocessableEntity());
        patchUser(token, teacher.getId(), Map.of("currentSemester", 3))
                .andExpect(status().isUnprocessableEntity());
        patchUser(token, teacher.getId(), Map.of("firstNames", "Docente editado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstNames").value("Docente editado"));
    }

    @Test
    void changingTheEmailClosesTheSessionsAndTheNewEmailLogsIn() throws Exception {
        Cookie session = refreshCookie(login(own.getEmail()).andExpect(status().isOk()).andReturn());
        String newEmail = email("cambiado");

        patchUser(loginToken(teacher), own.getId(), Map.of("email", newEmail)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(session)).andExpect(status().isUnauthorized());
        login(own.getEmail()).andExpect(status().isUnauthorized());
        login(newEmail).andExpect(status().isOk());
    }

    @Test
    void editingOtherDataKeepsTheSessionsOpen() throws Exception {
        Cookie session = refreshCookie(login(own.getEmail()).andExpect(status().isOk()).andReturn());

        patchUser(loginToken(teacher), own.getId(), Map.of("firstNames", "Otro nombre")).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void activationStillWorksAndYouCannotDeactivateYourself() throws Exception {
        String token = loginToken(teacher);

        patchUser(token, own.getId(), Map.of("active", false))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        patchUser(token, teacher.getId(), Map.of("active", false)).andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------ semestre en lote

    @Test
    void setsTheSemesterOfSeveralStudents() throws Exception {
        setSemester(loginToken(teacher), List.of(own.getId(), ownToo.getId()), 6)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        userRepository.flush();
        assertThat(studentRepository.findById(own.getId()).orElseThrow().getCurrentSemester()).isEqualTo(6);
        assertThat(studentRepository.findById(ownToo.getId()).orElseThrow().getCurrentSemester()).isEqualTo(6);
        assertThat(studentRepository.findById(foreign.getId()).orElseThrow().getCurrentSemester()).isEqualTo(2);
    }

    @Test
    void theAdminCanSetTheSemesterOfAnyStudentOfTheFaculty() throws Exception {
        setSemester(loginToken(admin), List.of(own.getId(), foreign.getId()), 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
    }

    @Test
    void aForeignStudentMakesItAllOrNothingWith403() throws Exception {
        setSemester(loginToken(teacher), List.of(own.getId(), foreign.getId()), 9)
                .andExpect(status().isForbidden());

        userRepository.flush();
        assertThat(studentRepository.findById(own.getId()).orElseThrow().getCurrentSemester()).isEqualTo(2);
        assertThat(studentRepository.findById(foreign.getId()).orElseThrow().getCurrentSemester()).isEqualTo(2);
    }

    @Test
    void anUnknownIdIs404ANonStudentIs422AndTheBodyIsValidated() throws Exception {
        String token = loginToken(admin);

        setSemester(token, List.of(own.getId(), Long.MAX_VALUE), 3).andExpect(status().isNotFound());
        setSemester(token, List.of(own.getId(), teacher.getId()), 3).andExpect(status().isUnprocessableEntity());
        setSemester(token, List.of(own.getId(), own.getId()), 3).andExpect(status().isUnprocessableEntity());
        setSemester(token, List.of(), 3).andExpect(status().isBadRequest());
        setSemester(token, List.of(own.getId()), 0).andExpect(status().isBadRequest());

        userRepository.flush();
        assertThat(studentRepository.findById(own.getId()).orElseThrow().getCurrentSemester()).isEqualTo(2);
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions patchUser(String token, Long id, Map<String, Object> body) throws Exception {
        return mockMvc.perform(patch("/api/users/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions setSemester(String token, List<Long> ids, int semester) throws Exception {
        return mockMvc.perform(patch("/api/students/semester")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("studentIds", ids, "currentSemester", semester))));
    }

    private Student student(String lastNames, Teacher creator) {
        Student student = new Student("Estudiante", lastNames, DocumentType.TI, document(), email("estudiante"),
                passwordEncoder.encode(PASSWORD), now(), "E" + SEQUENCE.incrementAndGet(), 2, program);
        student.setCreatedBy(creator);
        return persist(student);
    }

    private <T extends User> T persist(T user) {
        return userRepository.saveAndFlush(user);
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))));
    }

    private String loginToken(User user) throws Exception {
        MvcResult result = login(user.getEmail()).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(RefreshCookie.NAME);
        assertThat(cookie).isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String document() {
        return "55" + SEQUENCE.incrementAndGet();
    }

    private static String email(String prefix) {
        return prefix + ".edicion" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
    }
}
