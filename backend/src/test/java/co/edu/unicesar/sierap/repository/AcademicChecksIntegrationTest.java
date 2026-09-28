package co.edu.unicesar.sierap.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de las restricciones {@code CHECK} de la migración V5 contra
 * PostgreSQL real (Testcontainers): la base de datos rechaza, aunque un service no lo
 * valide, momentos con rangos o ventanas invertidas, puntajes negativos y periodos
 * con fechas incoherentes. Cada caso modifica la semilla de V2 y se revierte al
 * terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AcademicChecksIntegrationTest {

    /** Momento 1 de Ingeniería de Sistemas en 2026-2, cargado por V2. */
    private static final String SEED_MOMENT = "(SELECT MIN(id) FROM moment)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(strings = {
            "semester_from = 4, semester_to = 3",
            "semester_from = 0",
            "min_members = 6, max_members = 5",
            "min_members = 0",
            "number = 0",
            "group_formation_start = DATE '2026-09-15', group_formation_end = DATE '2026-09-01'",
            "submission_start = DATE '2026-10-31', submission_end = DATE '2026-10-01'",
            "grading_start = DATE '2026-11-20', grading_end = DATE '2026-11-02'",
            "group_formation_end = DATE '2026-10-01', submission_start = DATE '2026-10-01'",
            "submission_end = DATE '2026-11-02', grading_start = DATE '2026-11-02'"
    })
    void rejectsAnInvalidMoment(String assignments) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE moment SET " + assignments + " WHERE id = " + SEED_MOMENT))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsAMomentWithOrderedWindows() {
        int updated = jdbcTemplate.update("UPDATE moment SET "
                + "group_formation_start = DATE '2026-08-10', group_formation_end = DATE '2026-08-24', "
                + "submission_start = DATE '2026-10-01', submission_end = DATE '2026-11-20', "
                + "grading_start = DATE '2026-11-21', grading_end = DATE '2026-11-28' "
                + "WHERE id = " + SEED_MOMENT);

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void rejectsANegativeScore() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE performance_level SET score = -0.01 WHERE id = (SELECT MIN(id) FROM performance_level)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "start_date = DATE '2026-12-06'",
            "upc_evaluation_deadline = DATE '2026-12-06'",
            "upc_evaluation_deadline = DATE '2026-08-02'"
    })
    void rejectsAnInvalidPeriod(String assignment) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE academic_period SET " + assignment + " WHERE code = '2026-2'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
