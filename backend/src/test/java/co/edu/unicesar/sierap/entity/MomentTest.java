package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las ventanas de fechas, del lanzamiento y del puntaje máximo
 * de {@link Moment} (ADR-012).
 */
class MomentTest {

    /** Periodo 2026-2 con límite de evaluación de la UPC el 28 de noviembre. */
    private static final AcademicPeriod PERIOD = new AcademicPeriod("2026-2",
            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5), LocalDate.of(2026, 11, 28));

    private Moment moment;

    @BeforeEach
    void setUp() {
        moment = newMoment();
        moment.setGroupFormationStart(LocalDate.of(2026, 9, 1));
        moment.setGroupFormationEnd(LocalDate.of(2026, 9, 15));
        moment.setSubmissionStart(LocalDate.of(2026, 10, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 31));
        moment.setGradingStart(LocalDate.of(2026, 11, 2));
        moment.setGradingEnd(LocalDate.of(2026, 11, 20));
        moment.addLearningOutcome(outcomeWithOneCriterion("RAP1", "0.60"));
    }

    @Test
    void startsInConfiguration() {
        assertThat(newMoment().getStatus()).isEqualTo(MomentStatus.CONFIGURATION);
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
        moment.launch();

        assertThat(moment.isInGroupFormation(LocalDate.of(2026, 9, 10))).isFalse();
    }

    @Test
    void acceptsSubmissionsOnlyWhenLaunchedAndInsideTheWindow() {
        LocalDate inside = LocalDate.of(2026, 10, 15);
        assertThat(moment.isInSubmission(inside)).isFalse();

        moment.launch();

        assertThat(moment.isInSubmission(inside)).isTrue();
        assertThat(moment.isInSubmission(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(moment.isInSubmission(LocalDate.of(2026, 11, 1))).isFalse();
    }

    @Test
    void allowsGradingOnlyWhenLaunchedAndInsideTheWindow() {
        LocalDate inside = LocalDate.of(2026, 11, 10);
        assertThat(moment.isInGrading(inside)).isFalse();

        moment.launch();

        assertThat(moment.isInGrading(inside)).isTrue();
        assertThat(moment.isInGrading(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(moment.isInGrading(LocalDate.of(2026, 11, 21))).isFalse();
    }

    @Test
    void withoutWindowsNoPhaseIsOpen() {
        Moment withoutDates = newMoment();
        LocalDate today = LocalDate.of(2026, 9, 24);

        assertThat(withoutDates.isInGroupFormation(today)).isFalse();
        assertThat(withoutDates.isInSubmission(today)).isFalse();
        assertThat(withoutDates.isInGrading(today)).isFalse();
    }

    @Test
    void launchesACompleteConfiguration() {
        moment.launch();

        assertThat(moment.getStatus()).isEqualTo(MomentStatus.LAUNCHED);
    }

    @Test
    void cannotBeLaunchedTwice() {
        moment.launch();

        assertThatThrownBy(moment::launch)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya fue lanzado");
    }

    @Test
    void cannotBeLaunchedWithoutEveryWindow() {
        moment.setGradingEnd(null);

        assertLaunchFails("tres ventanas");
    }

    @Test
    void cannotBeLaunchedWithAWindowThatEndsBeforeItStarts() {
        moment.setSubmissionEnd(LocalDate.of(2026, 9, 30));

        assertLaunchFails("ventana de entregas no puede terminar antes de empezar");
    }

    @Test
    void cannotBeLaunchedWithOverlappingWindows() {
        moment.setGroupFormationEnd(LocalDate.of(2026, 10, 1));

        assertLaunchFails("después de la de conformación");

        moment.setGroupFormationEnd(LocalDate.of(2026, 9, 15));
        moment.setGradingStart(LocalDate.of(2026, 10, 31));

        assertLaunchFails("después de la de entregas");
    }

    @Test
    void cannotBeLaunchedIfGradingEndsAfterTheUpcDeadline() {
        moment.setGradingEnd(LocalDate.of(2026, 11, 29));

        assertLaunchFails("2026-11-28");
    }

    @Test
    void cannotBeLaunchedWithoutLearningOutcomesCriteriaOrLevels() {
        LearningOutcome rap1 = moment.getLearningOutcomes().get(0);
        moment.removeLearningOutcome(rap1);
        assertLaunchFails("al menos un RAP");

        moment.addLearningOutcome(new LearningOutcome("RAP2", "Sin criterios"));
        assertLaunchFails("RAP2 debe tener al menos un criterio");

        Moment withoutLevels = newMoment();
        copyWindows(withoutLevels);
        LearningOutcome outcome = new LearningOutcome("RAP3", "Criterio sin niveles");
        outcome.addCriterion(new EvaluationCriterion("Exposición", 1));
        withoutLevels.addLearningOutcome(outcome);
        assertThatThrownBy(withoutLevels::launch)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("niveles de desempeño");
    }

    @Test
    void locksTheConfigurationOnceLaunched() {
        moment.launch();

        assertThatThrownBy(() -> moment.setMaxMembers(10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> moment.setGradingEnd(LocalDate.of(2026, 11, 25)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> moment.addLearningOutcome(new LearningOutcome("RAP9", "Nuevo")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> moment.removeLearningOutcome(moment.getLearningOutcomes().get(0)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(moment.getMaxMembers()).isEqualTo(5);
    }

    @Test
    void maxScoreAddsUpTheLearningOutcomesAndIsZeroWithoutThem() {
        assertThat(newMoment().getMaxScore()).isEqualByComparingTo("0");

        moment.addLearningOutcome(outcomeWithOneCriterion("RAP2", "0.80"));

        assertThat(moment.getMaxScore()).isEqualByComparingTo("1.40");
    }

    private void assertLaunchFails(String message) {
        assertThatThrownBy(moment::launch)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(message);
        assertThat(moment.getStatus()).isEqualTo(MomentStatus.CONFIGURATION);
    }

    private void copyWindows(Moment target) {
        target.setGroupFormationStart(moment.getGroupFormationStart());
        target.setGroupFormationEnd(moment.getGroupFormationEnd());
        target.setSubmissionStart(moment.getSubmissionStart());
        target.setSubmissionEnd(moment.getSubmissionEnd());
        target.setGradingStart(moment.getGradingStart());
        target.setGradingEnd(moment.getGradingEnd());
    }

    private static Moment newMoment() {
        return new Moment(PERIOD, 1, "Momento 1", "Programación II (SS300)", 1, 3, "Bajo", 3, 5);
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
