package co.edu.unicesar.sierap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.PasswordTokenRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la carga masiva de estudiantes por CSV
 * ({@code POST /api/students/bulk/preview} y {@code /bulk}) contra PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class StudentImportIntegrationTest {

    private static final String PASSWORD = "Clave-segura-1";
    private static final String HEADER = "nombres,apellidos,tipo_documento,numero_documento,correo,"
            + "codigo_estudiantil,semestre";
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
    private PasswordTokenRepository passwordTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private EmailService emailService;

    private Faculty faculty;
    private Program program;
    private Teacher teacher;
    private int n;

    @BeforeEach
    void createScenario() {
        n = SEQUENCE.incrementAndGet();
        faculty = new Faculty("IM" + n, "Facultad de carga " + n);
        program = new Program("PI" + n, "Programa de carga " + n);
        faculty.addProgram(program);
        facultyRepository.saveAndFlush(faculty);
        teacher = persist(new Teacher("Docente", "Carga " + n, DocumentType.CC, "66" + n, "docente.carga" + n
                + "@unicesar.edu.co", passwordEncoder.encode(PASSWORD), LocalDateTime.now(clock), faculty));
    }

    /** CSV con una fila válida, una con el correo repetido dentro del archivo y una con errores de campo. */
    private String sampleCsv() {
        return HEADER + "\n"
                + "Ana,Pérez,CC,7" + n + "1,ana" + n + "@unicesar.edu.co,K" + n + "1,3\n"
                + "Luis,Gómez,TI,7" + n + "2,ana" + n + "@unicesar.edu.co,K" + n + "2,2\n"
                + "María,,XX,7" + n + "3,maria" + n + "@unicesar.edu.co,K" + n + "3,0\n"
                + "Pedro,Ruiz,CE,7" + n + "4,pedro" + n + "@unicesar.edu.co,K" + n + "4,5\n";
    }

    @Test
    void previewReportsEveryRowAndWritesNothing() throws Exception {
        long before = userRepository.count();

        preview(loginToken(), sampleCsv(), "estudiantes.csv")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(2))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.rows[0].row").value(2))
                .andExpect(jsonPath("$.rows[0].errors.length()").value(0))
                .andExpect(jsonPath("$.rows[1].row").value(3))
                .andExpect(jsonPath("$.rows[1].errors[0]").value(
                        "El correo ana" + n + "@unicesar.edu.co está repetido en el archivo (ya aparece en la fila 2)."))
                .andExpect(jsonPath("$.rows[2].errors.length()").value(3))
                .andExpect(jsonPath("$.rows[3].documentType").value("CE"));

        assertThat(userRepository.count()).isEqualTo(before);
        verify(emailService, never()).sendPasswordLink(any(), any(), any(), anyInt());
    }

    @Test
    void previewFlagsValuesThatAlreadyExistInTheDatabase() throws Exception {
        String existing = HEADER + "\nAna,Pérez,CC," + teacher.getDocumentNumber() + "," + teacher.getEmail()
                + ",K" + n + "9,1\n";

        preview(loginToken(), existing, "estudiantes.csv")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(0))
                .andExpect(jsonPath("$.rows[0].errors[0]").value(
                        "Ya existe una cuenta con el correo " + teacher.getEmail() + "."))
                .andExpect(jsonPath("$.rows[0].errors[1]").value(
                        "Ya existe una cuenta con el documento " + teacher.getDocumentNumber() + "."));
    }

    @Test
    void bulkCreatesOnlyTheValidRowsAndReportsTheOthers() throws Exception {
        String token = loginToken();
        long tokensBefore = passwordTokenRepository.count();

        bulk(token, sampleCsv(), "estudiantes.csv")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[1].row").value(4));

        userRepository.flush();
        Student ana = studentRepository.findByStudentCode("K" + n + "1").orElseThrow();
        assertThat(ana.getEmail()).isEqualTo("ana" + n + "@unicesar.edu.co");
        assertThat(ana.getCurrentSemester()).isEqualTo(3);
        assertThat(ana.getProgram().getId()).isEqualTo(program.getId());
        assertThat(ana.getCreatedBy().getId()).isEqualTo(teacher.getId());
        assertThat(ana.hasPassword()).isFalse();
        assertThat(studentRepository.findByStudentCode("K" + n + "2")).isEmpty();
        assertThat(studentRepository.findByStudentCode("K" + n + "4")).isPresent();
        // Cada estudiante creado recibe su enlace para crear la contraseña. El correo sale
        // después del commit (PasswordLinkServiceImpl), que esta prueba @Transactional no hace:
        // aquí se comprueba el token de cada uno; el envío lo cubre PasswordLinkServiceImplTest.
        assertThat(passwordTokenRepository.count()).isEqualTo(tokensBefore + 2);
    }

    @Test
    void supportsSemicolonSeparatorAndBom() throws Exception {
        String csv = "﻿" + HEADER.replace(',', ';') + "\nAna;Pérez;cc;7" + n + "1;ana" + n
                + "@unicesar.edu.co;K" + n + "1;3\n";

        bulk(loginToken(), csv, "estudiantes.csv")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1));
    }

    @Test
    void invalidFilesAre422() throws Exception {
        String token = loginToken();

        preview(token, sampleCsv(), "estudiantes.txt").andExpect(status().isUnprocessableEntity());
        preview(token, HEADER + "\n", "estudiantes.csv").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El archivo no tiene estudiantes."));
        preview(token, "nombres,apellidos\nAna,Pérez\n", "estudiantes.csv")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Faltan columnas en la cabecera")));
        preview(token, HEADER + "\n" + "x".repeat(1024 * 1024 + 1), "estudiantes.csv")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El archivo no puede pesar más de 1 MB."));
    }

    @Test
    void aTeacherOfAnotherFacultyIs403AndAnUnknownProgramIs404() throws Exception {
        Faculty other = facultyRepository.saveAndFlush(new Faculty("OF" + n, "Otra facultad " + n));
        Teacher outsider = persist(new Teacher("Docente", "Ajeno", DocumentType.CC, "67" + n,
                "ajeno.carga" + n + "@unicesar.edu.co", passwordEncoder.encode(PASSWORD),
                LocalDateTime.now(clock), other));
        long before = userRepository.count();

        bulk(loginToken(outsider), sampleCsv(), "estudiantes.csv").andExpect(status().isForbidden());
        preview(loginToken(outsider), sampleCsv(), "estudiantes.csv").andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/students/bulk/preview").param("programId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken()))
                .andExpect(status().isNotFound());

        assertThat(userRepository.count()).isEqualTo(before);
    }

    // ------------------------------------------------------------ utilidades

    private ResultActions preview(String token, String csv, String filename) throws Exception {
        return upload("/api/students/bulk/preview", token, csv, filename);
    }

    private ResultActions bulk(String token, String csv, String filename) throws Exception {
        return upload("/api/students/bulk", token, csv, filename);
    }

    private ResultActions upload(String url, String token, String csv, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(multipart(url).file(file).param("programId", program.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private <T extends User> T persist(T user) {
        return userRepository.saveAndFlush(user);
    }

    private String loginToken() throws Exception {
        return loginToken(teacher);
    }

    private String loginToken(User user) throws Exception {
        ResultActions login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))));
        return JsonPath.read(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.accessToken");
    }
}
