package co.edu.unicesar.sierap.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la migración V3 contra PostgreSQL real (Testcontainers):
 * herencia JOINED de {@link User}, discriminador {@code role}, catálogo de tipos de
 * evaluador y las restricciones de la base de datos (ADR-009, ADR-011, ADR-016).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class UserIntegrationTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 25, 8, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Faculty faculty;
    private Program program;

    @BeforeEach
    void loadSeedData() {
        faculty = facultyRepository.findByCode("04").orElseThrow();
        program = programRepository.findByCode("108").orElseThrow();
    }

    @Test
    void savesEachRoleInItsOwnTableWithTheRoleAsDiscriminator() {
        EvaluatorType alumnus = evaluatorTypeRepository.findByName("Egresado").orElseThrow();
        ExternalEvaluator evaluator = new ExternalEvaluator("Sofía", "Vega", DocumentType.CC, "2005",
                "sofia@correo.com", "hash", CREATED_AT, alumnus);
        evaluator.setGraduationYear(2018);
        evaluator.setGraduationProgram(program);

        User superAdmin = userRepository.save(new SuperAdmin("Ana", "Pérez", DocumentType.CC, "2001",
                "ana@unicesar.edu.co", "hash", CREATED_AT));
        User admin = userRepository.save(new Admin("Luis", "Gómez", DocumentType.CC, "2002",
                "luis@unicesar.edu.co", "hash", CREATED_AT, faculty));
        User teacher = userRepository.save(new Teacher("Marta", "Díaz", DocumentType.CC, "2003",
                "marta@unicesar.edu.co", "hash", CREATED_AT, faculty));
        User student = userRepository.save(new Student("Juan", "Rojas", DocumentType.TI, "2004",
                "juan@unicesar.edu.co", "hash", CREATED_AT, "20261001", 3, program));
        User external = userRepository.save(evaluator);
        userRepository.flush();

        assertThat(roleInDatabase(superAdmin)).isEqualTo("SUPER_ADMIN");
        assertThat(roleInDatabase(admin)).isEqualTo("ADMIN");
        assertThat(roleInDatabase(teacher)).isEqualTo("TEACHER");
        assertThat(roleInDatabase(student)).isEqualTo("STUDENT");
        assertThat(roleInDatabase(external)).isEqualTo("EXTERNAL_EVALUATOR");
        assertThat(rowsWithId("teacher", teacher.getId())).isEqualTo(1);
        assertThat(rowsWithId("student", student.getId())).isEqualTo(1);
        assertThat(rowsWithId("external_evaluator", external.getId())).isEqualTo(1);
    }

    @Test
    void findsUsersByEmailAsTheirConcreteSubclass() {
        userRepository.saveAndFlush(new Teacher("Marta", "Díaz", DocumentType.CC, "2003",
                "Marta@UniCesar.edu.co", "hash", CREATED_AT, faculty));

        assertThat(userRepository.findByEmail("marta@unicesar.edu.co"))
                .get().isInstanceOf(Teacher.class);
        assertThat(teacherRepository.findByFacultyId(faculty.getId())).hasSize(1);
    }

    @Test
    void aFacultyCanHaveSeveralAdmins() {
        userRepository.save(new Admin("Luis", "Gómez", DocumentType.CC, "2002",
                "luis@unicesar.edu.co", "hash", CREATED_AT, faculty));
        userRepository.save(new Admin("Carla", "Ruiz", DocumentType.CC, "2006",
                "carla@unicesar.edu.co", "hash", CREATED_AT, faculty));
        userRepository.flush();

        assertThat(adminRepository.findByFacultyId(faculty.getId())).hasSize(2);
    }

    @Test
    void seedsTheEvaluatorTypeCatalog() {
        assertThat(evaluatorTypeRepository.findByActiveTrueOrderByNameAsc())
                .extracting(EvaluatorType::getName)
                .containsExactly("Egresado", "Empresario");
    }

    @Test
    void rejectsADuplicateEmailEvenWithDifferentCase() {
        userRepository.saveAndFlush(new Teacher("Marta", "Díaz", DocumentType.CC, "2003",
                "marta@unicesar.edu.co", "hash", CREATED_AT, faculty));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new Teacher("Otra", "Persona", DocumentType.CC,
                "2099", "MARTA@unicesar.edu.co", "hash", CREATED_AT, faculty)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsASemesterBelowOne() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(new Student("Juan", "Rojas", DocumentType.TI,
                "2004", "juan@unicesar.edu.co", "hash", CREATED_AT, "20261001", 0, program)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aFacultyWithUsersCannotBeDeleted() {
        userRepository.saveAndFlush(new Teacher("Marta", "Díaz", DocumentType.CC, "2003",
                "marta@unicesar.edu.co", "hash", CREATED_AT, faculty));
        // Como en una petición nueva: el docente no está cargado en la sesión, así que
        // quien bloquea el borrado es la llave foránea de PostgreSQL (ADR-016).
        entityManager.clear();
        Faculty facultyToDelete = facultyRepository.findByCode("04").orElseThrow();

        assertThatThrownBy(() -> {
            facultyRepository.delete(facultyToDelete);
            facultyRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private String roleInDatabase(User user) {
        return jdbcTemplate.queryForObject("SELECT role FROM app_user WHERE id = ?", String.class, user.getId());
    }

    private int rowsWithId(String table, Long id) {
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class, id);
        return rows == null ? 0 : rows;
    }
}
