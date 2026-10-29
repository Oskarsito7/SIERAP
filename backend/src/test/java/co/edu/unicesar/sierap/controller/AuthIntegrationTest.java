package co.edu.unicesar.sierap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.RefreshCookie;
import co.edu.unicesar.sierap.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

/**
 * Prueba de integración de la Etapa 2 (ADR-017): inicio de sesión, rotación del
 * token de renovación, contraseña temporal, enlaces por correo, creación de cuentas
 * con su alcance y registro con código, contra PostgreSQL real.
 *
 * <p>El envío de correos se simula para capturar el enlace y usarlo como lo haría la
 * persona que lo recibe. Cada prueba crea sus propios usuarios con datos únicos, así
 * que no dependen del orden en que se ejecuten.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final String SEEDED_SUPER_ADMIN = "sierap@unicesar.edu.co";
    private static final String SEEDED_TEMPORARY_PASSWORD = "Sierap-Inicio-2026";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    private Faculty engineering;
    private Program systems;

    @BeforeEach
    void loadSeedData() {
        engineering = facultyRepository.findByCode("04").orElseThrow();
        systems = programRepository.findByCode("108").orElseThrow();
    }

    @Test
    void seededSuperAdminMustChangeTheTemporaryPasswordBeforeUsingTheApi() throws Exception {
        MvcResult login = login(SEEDED_SUPER_ADMIN, SEEDED_TEMPORARY_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andReturn();
        String temporaryToken = accessToken(login);

        mockMvc.perform(get("/api/evaluator-types").header(HttpHeaders.AUTHORIZATION, bearer(temporaryToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Debe cambiar su contraseña temporal antes de continuar."));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(temporaryToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(SEEDED_SUPER_ADMIN));

        mockMvc.perform(post("/api/auth/password/change")
                        .header(HttpHeaders.AUTHORIZATION, bearer(temporaryToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", "otra-clave-1", "newPassword", PASSWORD)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La contraseña actual no es correcta."));

        MvcResult changed = mockMvc.perform(post("/api/auth/password/change")
                        .header(HttpHeaders.AUTHORIZATION, bearer(temporaryToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", SEEDED_TEMPORARY_PASSWORD, "newPassword", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(false))
                .andReturn();

        mockMvc.perform(get("/api/evaluator-types").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(changed))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Egresado"));
        // Cambiar la contraseña cierra las demás sesiones: la cookie anterior ya no sirve.
        refresh(refreshCookie(login)).andExpect(status().isUnauthorized());
        refresh(refreshCookie(changed)).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameMessage() throws Exception {
        User teacher = persistTeacher(engineering);

        login(teacher.getEmail(), "Clave-incorrecta-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
        login("nadie@unicesar.edu.co", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }

    @Test
    void refreshRotatesTheCookieAndReusingAnOldOneClosesEverySession() throws Exception {
        User teacher = persistTeacher(engineering);
        Cookie first = refreshCookie(login(teacher.getEmail(), PASSWORD).andReturn());

        MvcResult renewed = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(teacher.getEmail()))
                .andReturn();
        Cookie second = refreshCookie(renewed);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        // La cookie ya rotada vuelve a llegar: se asume robada y se cierran todas las sesiones.
        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("La sesión expiró. Inicie sesión de nuevo."));
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieAnswers401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logoutClosesOnlyThatSessionAndClearsTheCookie() throws Exception {
        User teacher = persistTeacher(engineering);
        Cookie laptop = refreshCookie(login(teacher.getEmail(), PASSWORD).andReturn());
        Cookie phone = refreshCookie(login(teacher.getEmail(), PASSWORD).andReturn());

        mockMvc.perform(post("/api/auth/logout").cookie(laptop))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        refresh(laptop).andExpect(status().isUnauthorized());
        // Una cookie de una sesión cerrada no es un token robado: la otra sesión sigue viva.
        refresh(phone).andExpect(status().isOk());
    }

    @Test
    void protectedRoutesAnswer401WithoutAValidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Debe iniciar sesión para continuar."))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("token-falso")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesATeacherWhoSetsThePasswordWithTheEmailedLink() throws Exception {
        User admin = persistAdmin(engineering);
        String adminToken = loginToken(admin.getEmail());
        String email = "docente" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";

        mockMvc.perform(post("/api/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser(email, "facultyId", engineering.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.email").value(email));

        // Sin contraseña todavía: no puede iniciar sesión hasta usar el enlace.
        login(email, PASSWORD).andExpect(status().isUnauthorized());

        String linkToken = capturedLinkToken(email, PasswordTokenPurpose.ACCOUNT_SETUP);
        setPassword(linkToken, PASSWORD).andExpect(status().isNoContent());
        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(false));
        // El enlace es de un solo uso.
        setPassword(linkToken, "Otra-clave-2")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El enlace no es válido o ya venció. Solicite uno nuevo."));
    }

    @Test
    void adminCannotCreateTeachersOfAnotherFaculty() throws Exception {
        Faculty other = facultyRepository.save(new Faculty("9" + SEQUENCE.incrementAndGet(), "Facultad de prueba"));
        User admin = persistAdmin(other);
        String adminToken = loginToken(admin.getEmail());

        mockMvc.perform(post("/api/teachers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser("intruso" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co",
                                "facultyId", engineering.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo puede gestionar información de su facultad."));
    }

    @Test
    void teacherCannotCreateAdmins() throws Exception {
        User teacher = persistTeacher(engineering);

        mockMvc.perform(post("/api/admins")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(teacher.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser("admin" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co",
                                "facultyId", engineering.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación."));
    }

    @Test
    void onlyTheSuperAdminListsTheAdminsOfAFaculty() throws Exception {
        int n = SEQUENCE.incrementAndGet();
        Faculty faculty = facultyRepository.save(new Faculty("A" + n, "Facultad de administradores " + n));
        User admin = persistAdmin(faculty);
        User superAdmin = userRepository.save(new SuperAdmin("Super", "Prueba " + n, DocumentType.CC, "60" + n,
                "super.prueba" + n + "@unicesar.edu.co", passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock)));

        mockMvc.perform(get("/api/admins").param("facultyId", faculty.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(superAdmin.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", contains(admin.getEmail())))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
        mockMvc.perform(get("/api/admins").param("facultyId", faculty.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(admin.getEmail()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgotPasswordSendsALinkOnlyWhenTheAccountExists() throws Exception {
        User teacher = persistTeacher(engineering);

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "nadie@unicesar.edu.co")))
                .andExpect(status().isNoContent());
        verify(emailService, never()).sendPasswordLink(any(), any(), any(), anyInt());

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", teacher.getEmail().toUpperCase())))
                .andExpect(status().isNoContent());
        String linkToken = capturedLinkToken(teacher.getEmail(), PasswordTokenPurpose.PASSWORD_RESET);

        setPassword(linkToken, "Clave-nueva-2").andExpect(status().isNoContent());
        login(teacher.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        login(teacher.getEmail(), "Clave-nueva-2").andExpect(status().isOk());
    }

    @Test
    void weakPasswordIsRejectedWithTheFieldMessage() throws Exception {
        setPassword("cualquier-enlace", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").value(
                        "La contraseña debe tener al menos 8 caracteres, con al menos una letra y un número."));
    }

    @Test
    void studentRegistersWithTheCodeOfTheirTeacherUntilItIsDeactivated() throws Exception {
        User teacher = persistTeacher(engineering);
        String teacherToken = loginToken(teacher.getEmail());

        MvcResult created = mockMvc.perform(post("/api/registration-codes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("programId", systems.getId(),
                                "expiresOn", LocalDate.now(clock).plusDays(10).toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programName").value("Ingeniería de Sistemas"))
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String code = JsonPath.read(created.getResponse().getContentAsString(), "$.code");
        Integer codeId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(code).hasSize(8);

        register(code.toLowerCase(), "personal" + SEQUENCE.incrementAndGet() + "@gmail.com")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("correo institucional")));

        String email = "estudiante" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co";
        register(code.toLowerCase(), email).andExpect(status().isAccepted());
        capturedLinkToken(email, PasswordTokenPurpose.ACCOUNT_SETUP);

        mockMvc.perform(get("/api/registration-codes").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(code))
                .andExpect(jsonPath("$[0].registeredCount").value(1));

        mockMvc.perform(patch("/api/registration-codes/" + codeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("active", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        register(code, "estudiante" + SEQUENCE.incrementAndGet() + "@unicesar.edu.co")
                .andExpect(status().isUnprocessableEntity());
    }

    // ----------------------------------------------------------------- utilidades

    private User persistTeacher(Faculty faculty) {
        int n = SEQUENCE.incrementAndGet();
        return userRepository.save(new Teacher("Docente", "Prueba " + n, DocumentType.CC, "70" + n,
                "docente.prueba" + n + "@unicesar.edu.co", passwordEncoder.encode(PASSWORD),
                LocalDateTime.now(clock), faculty));
    }

    private User persistAdmin(Faculty faculty) {
        int n = SEQUENCE.incrementAndGet();
        return userRepository.save(new Admin("Admin", "Prueba " + n, DocumentType.CC, "80" + n,
                "admin.prueba" + n + "@unicesar.edu.co", passwordEncoder.encode(PASSWORD),
                LocalDateTime.now(clock), faculty));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", password)));
    }

    private String loginToken(String email) throws Exception {
        return accessToken(login(email, PASSWORD).andExpect(status().isOk()).andReturn());
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(cookie));
    }

    private ResultActions setPassword(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("token", token, "newPassword", newPassword)));
    }

    private ResultActions register(String code, String email) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("registrationCode", code, "firstNames", "Estudiante", "lastNames", "Prueba " + n,
                        "documentType", "TI", "documentNumber", "90" + n, "email", email,
                        "studentCode", "2026" + n, "currentSemester", 2)));
    }

    /** Valor del enlace que se envió por correo a esa cuenta con ese propósito. */
    private String capturedLinkToken(String email, PasswordTokenPurpose purpose) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordLink(argThat(user -> user.getEmail().equals(email)), eq(purpose),
                link.capture(), anyInt());
        assertThat(link.getValue()).startsWith("http://localhost:5173/set-password?token=");
        return link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(RefreshCookie.NAME);
        assertThat(cookie).isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String newUser(String email, String extraField, Object extraValue) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return json("firstNames", "Nuevo", "lastNames", "Usuario " + n, "documentType", "CC",
                "documentNumber", "60" + n, "email", email, extraField, extraValue);
    }

    /** Arma un JSON a partir de pares nombre, valor. */
    private String json(Object... pairs) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            body.put((String) pairs[i], pairs[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }
}
