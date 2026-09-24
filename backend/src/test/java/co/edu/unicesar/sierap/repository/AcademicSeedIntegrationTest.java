package co.edu.unicesar.sierap.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of migrations V1 and V2 against a real PostgreSQL
 * (Testcontainers): Flyway creates the schema, Hibernate validates it against the
 * entities and the seed leaves Momento 1 complete with the official rubric.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AcademicSeedIntegrationTest {

    private static final String PROGRAM_CODE = "SIS";
    private static final String PERIOD_CODE = "2026-2";

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private MomentRepository momentRepository;

    @Test
    void loadsTheProgramWithItsFacultyAndThePeriod() {
        Program program = programRepository.findByCode(PROGRAM_CODE).orElseThrow();
        AcademicPeriod period = academicPeriodRepository.findByCode(PERIOD_CODE).orElseThrow();

        assertThat(program.getFaculty().getPrograms()).contains(program);
        assertThat(period.contains(period.getUpcEvaluationDeadline())).isTrue();
    }

    @Test
    void moment1IsInConfigurationWithoutDates() {
        Moment moment = moment1();

        assertThat(moment.getStatus()).isEqualTo(MomentStatus.CONFIGURATION);
        assertThat(moment.getGroupFormationStart()).isNull();
        assertThat(moment.getGradingEnd()).isNull();
    }

    @Test
    void moment1HasTheFiveLearningOutcomesWithTheirCriteriaAndFourLevels() {
        List<LearningOutcome> outcomes = moment1().getLearningOutcomes();

        assertThat(outcomes).extracting(LearningOutcome::getCode)
                .containsExactly("RAP1", "RAP2", "RAP3", "RAP4", "RAP5");
        assertThat(outcomes).extracting(outcome -> outcome.getCriteria().size())
                .containsExactly(1, 2, 1, 2, 2);
        assertThat(outcomes).flatExtracting(LearningOutcome::getCriteria)
                .extracting(criterion -> criterion.getLevels().size())
                .containsOnly(4);
    }

    @Test
    void moment1AddsUpToFivePoints() {
        Moment moment = moment1();
        EvaluationCriterion defense = moment.getLearningOutcomes().get(4).getCriteria().get(1);

        assertThat(moment.getMaxScore()).isEqualByComparingTo("5.00");
        assertThat(defense.getMaxScore()).isEqualByComparingTo("0.80");
    }

    private Moment moment1() {
        return momentRepository
                .findByProgramCodeAndPeriodCodeAndNumber(PROGRAM_CODE, PERIOD_CODE, 1)
                .orElseThrow();
    }
}
