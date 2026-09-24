package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the date windows and the maximum score of {@link Moment}
 * (ADR-012).
 */
class MomentTest {

    private Moment moment;

    @BeforeEach
    void setUp() {
        moment = new Moment();
        moment.setGroupFormationStart(LocalDate.of(2026, 9, 1));
        moment.setGroupFormationEnd(LocalDate.of(2026, 9, 15));
        moment.setSubmissionStart(LocalDate.of(2026, 10, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
        moment.setGradingStart(LocalDate.of(2026, 11, 2));
        moment.setGradingEnd(LocalDate.of(2026, 11, 20));
    }

    @Test
    void startsInConfiguration() {
        assertThat(new Moment().getStatus()).isEqualTo(MomentStatus.CONFIGURATION);
    }

    @Test
    void isInGroupFormationInsideTheWindowWhileConfiguring() {
        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 9, 1))).isTrue();
        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 9, 15))).isTrue();
        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 9, 16))).isFalse();
        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 8, 31))).isFalse();
    }

    @Test
    void isNotInGroupFormationOnceLaunched() {
        moment.setStatus(MomentStatus.LAUNCHED);

        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 9, 10))).isFalse();
    }

    @Test
    void acceptsSubmissionsOnlyWhenLaunchedAndInsideTheWindow() {
        LocalDate inside = LocalDate.of(2026, 10, 15);
        assertThat(moment.isInSubmission(inside)).isFalse();

        moment.setStatus(MomentStatus.LAUNCHED);

        assertThat(moment.isInSubmission(inside)).isTrue();
        assertThat(moment.isInSubmission(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(moment.isInSubmission(LocalDate.of(2026, 11, 1))).isFalse();
    }

    @Test
    void allowsGradingOnlyWhenLaunchedAndInsideTheWindow() {
        LocalDate inside = LocalDate.of(2026, 11, 10);
        assertThat(moment.isInGrading(inside)).isFalse();

        moment.setStatus(MomentStatus.LAUNCHED);

        assertThat(moment.isInGrading(inside)).isTrue();
        assertThat(moment.isInGrading(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(moment.isInGrading(LocalDate.of(2026, 11, 21))).isFalse();
    }

    @Test
    void withoutWindowsNoPhaseIsOpen() {
        Moment withoutDates = new Moment();
        LocalDate today = LocalDate.of(2026, 9, 24);

        assertThat(withoutDates.isInGroupFormation(today)).isFalse();
        withoutDates.setStatus(MomentStatus.LAUNCHED);
        assertThat(withoutDates.isInSubmission(today)).isFalse();
        assertThat(withoutDates.isInGrading(today)).isFalse();
    }

    @Test
    void maxScoreAddsUpTheLearningOutcomesAndIsZeroWithoutThem() {
        assertThat(moment.getMaxScore()).isEqualByComparingTo("0");

        moment.addLearningOutcome(outcomeWithOneCriterion("RAP1", "0.60"));
        moment.addLearningOutcome(outcomeWithOneCriterion("RAP2", "0.80"));

        assertThat(moment.getMaxScore()).isEqualByComparingTo("1.40");
    }

    private static LearningOutcome outcomeWithOneCriterion(String code, String maxScore) {
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio", 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, BigDecimal.ZERO, 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(maxScore), 2));
        LearningOutcome outcome = new LearningOutcome(code, "Descripción");
        outcome.addCriterion(criterion);
        return outcome;
    }
}
