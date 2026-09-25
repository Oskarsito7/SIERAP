package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de la jerarquía {@link User}: cada rol responde su propio
 * {@code getRole()} (polimorfismo) y los datos comunes se comportan igual en todos.
 */
class UserTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 25, 8, 0);

    private final Faculty faculty = new Faculty("04", "Facultad de Ingenierías y Tecnologías");
    private final Program program = new Program("108", "Ingeniería de Sistemas");
    private final EvaluatorType businessType = new EvaluatorType("Empresario", null);

    @Test
    void eachSubclassReportsItsOwnRole() {
        List<User> users = List.of(
                new SuperAdmin("Ana", "Pérez", DocumentType.CC, "1001", "ana@unicesar.edu.co", "hash", CREATED_AT),
                new Admin("Luis", "Gómez", DocumentType.CC, "1002", "luis@unicesar.edu.co", "hash", CREATED_AT,
                        faculty),
                new Teacher("Marta", "Díaz", DocumentType.CC, "1003", "marta@unicesar.edu.co", "hash", CREATED_AT,
                        faculty),
                new Student("Juan", "Rojas", DocumentType.TI, "1004", "juan@unicesar.edu.co", "hash", CREATED_AT,
                        "20261001", 3, program),
                new ExternalEvaluator("Sofía", "Vega", DocumentType.CE, "1005", "sofia@empresa.com", "hash",
                        CREATED_AT, businessType));

        assertThat(users).extracting(User::getRole).containsExactly(
                UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.TEACHER, UserRole.STUDENT,
                UserRole.EXTERNAL_EVALUATOR);
    }

    @Test
    void fullNameJoinsFirstAndLastNames() {
        Teacher teacher = new Teacher("Marta Lucía", "Díaz Ortiz", DocumentType.CC, "1003",
                "marta@unicesar.edu.co", "hash", CREATED_AT, faculty);

        assertThat(teacher.getFullName()).isEqualTo("Marta Lucía Díaz Ortiz");
    }

    @Test
    void emailIsStoredTrimmedAndInLowerCase() {
        Student student = new Student("Juan", "Rojas", DocumentType.TI, "1004", "  Juan.Rojas@UniCesar.edu.co ",
                "hash", CREATED_AT, "20261001", 3, program);

        assertThat(student.getEmail()).isEqualTo("juan.rojas@unicesar.edu.co");

        student.setEmail("OTRO@Correo.com");
        assertThat(student.getEmail()).isEqualTo("otro@correo.com");
    }

    @Test
    void newUsersAndEvaluatorTypesAreActive() {
        ExternalEvaluator evaluator = new ExternalEvaluator("Sofía", "Vega", DocumentType.CE, "1005",
                "sofia@empresa.com", "hash", CREATED_AT, businessType);

        assertThat(evaluator.isActive()).isTrue();
        assertThat(businessType.isActive()).isTrue();
        assertThat(evaluator.getCreatedAt()).isEqualTo(CREATED_AT);
    }
}
