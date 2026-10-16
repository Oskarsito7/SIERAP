package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del cálculo de notas de {@link Evaluation}, {@link CriterionGrade}
 * e {@link IndividualGrade}: suma por RAP y total, corrección de un criterio,
 * evaluación completa, paso de borrador a enviada y ajustes por estudiante (ADR-018).
 */
class EvaluationTest {

    private LearningOutcome rap1;
    private LearningOutcome rap5;
    private EvaluationCriterion problem;
    private EvaluationCriterion description;
    private EvaluationCriterion defense;
    private Evaluation evaluation;
    private Student ana;
    private Student luis;

    @BeforeEach
    void setUp() {
        problem = criterion("Identificación del problema", "0.60");
        description = criterion("Descripción de la solución", "0.60");
        defense = criterion("Sustentación", "0.80");
        rap1 = new LearningOutcome("RAP1", "INTEGRA...");
        rap1.addCriterion(problem);
        rap5 = new LearningOutcome("RAP5", "EMPRENDE...");
        rap5.addCriterion(description);
        rap5.addCriterion(defense);
        Moment moment = new Moment();
        moment.addLearningOutcome(rap1);
        moment.addLearningOutcome(rap5);

        ProjectGroup group = new ProjectGroup(moment, new Teacher(), "Grupo 1");
        ana = new Student();
        luis = new Student();
        group.addMember(ana);
        group.addMember(luis);
        evaluation = new Evaluation(new Teacher(), LocalDateTime.of(2026, 11, 10, 8, 0));
        group.addEvaluation(evaluation);
    }

    @Test
    void addsUpTheScoresPerLearningOutcomeAndInTotal() {
        evaluation.grade(level(problem, "Alto"));
        evaluation.grade(level(description, "Sobresaliente"));
        evaluation.grade(level(defense, "Básico"));

        assertThat(evaluation.getScoreFor(rap1)).isEqualByComparingTo("0.48");
        assertThat(evaluation.getScoreFor(rap5)).isEqualByComparingTo("1.08");
        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("1.56");
    }

    @Test
    void anEmptyEvaluationIsWorthZeroAndIsNotComplete() {
        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("0");
        assertThat(evaluation.getScoreFor(rap5)).isEqualByComparingTo("0");
        assertThat(evaluation.isComplete()).isFalse();
    }

    @Test
    void gradingACriterionAgainReplacesTheLevelInsteadOfAddingAnother() {
        evaluation.grade(level(problem, "Bajo"));
        CriterionGrade first = evaluation.getGrades().get(0);

        evaluation.grade(level(problem, "Sobresaliente"));

        assertThat(evaluation.getGrades()).containsExactly(first);
        assertThat(first.getLevel().getName()).isEqualTo("Sobresaliente");
        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("0.60");
    }

    @Test
    void isCompleteOnlyWhenEveryCriterionOfTheMomentIsGraded() {
        evaluation.grade(level(problem, "Alto"));
        evaluation.grade(level(description, "Alto"));
        assertThat(evaluation.isComplete()).isFalse();

        evaluation.grade(level(defense, "Alto"));

        assertThat(evaluation.isComplete()).isTrue();
    }

    @Test
    void keepsTheScoreOfTheLevelAtTheTimeOfGrading() {
        PerformanceLevel high = level(problem, "Alto");
        evaluation.grade(high);

        high.setScore(new BigDecimal("0.50"));

        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("0.48");
    }

    @Test
    void startsAsADraftAndCountsOnlyOnceSubmitted() {
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.DRAFT);
        assertThat(evaluation.isSubmitted()).isFalse();

        evaluation.submit();

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
        assertThat(evaluation.isSubmitted()).isTrue();
    }

    @Test
    void aSubmittedEvaluationCanStillBeCorrected() {
        evaluation.grade(level(problem, "Bajo"));
        evaluation.submit();

        evaluation.grade(level(problem, "Alto"));

        assertThat(evaluation.isSubmitted()).isTrue();
        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("0.48");
    }

    @Test
    void rejectsALevelOfAnotherCriterion() {
        evaluation.grade(level(problem, "Alto"));
        CriterionGrade grade = evaluation.getGrades().get(0);

        assertThatThrownBy(() -> grade.changeLevel(level(defense, "Alto")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------ ajustes individuales

    @Test
    void withoutAdjustmentsEveryMemberHasTheGroupScore() {
        gradeGroup("Alto", "Alto", "Alto");

        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo(evaluation.getTotalScore());
        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("1.60");
        assertThat(evaluation.getScoreFor(rap5, luis)).isEqualByComparingTo(evaluation.getScoreFor(rap5));
    }

    @Test
    void anAdjustmentReplacesTheGroupLevelOnlyForThatStudentAndCriterion() {
        gradeGroup("Alto", "Alto", "Alto");

        evaluation.adjust(ana, level(defense, "Sobresaliente"));

        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("1.76");
        assertThat(evaluation.getScoreFor(rap5, ana)).isEqualByComparingTo("1.28");
        assertThat(evaluation.getScoreFor(rap1, ana)).isEqualByComparingTo("0.48");
        assertThat(evaluation.getTotalScoreFor(luis)).isEqualByComparingTo("1.60");
        assertThat(evaluation.getTotalScore()).isEqualByComparingTo("1.60");
        assertThat(evaluation.getAdjustmentsFor(ana)).hasSize(1);
        assertThat(evaluation.getAdjustmentsFor(luis)).isEmpty();
    }

    @Test
    void adjustingTheSameCriterionAgainChangesTheSameAdjustment() {
        gradeGroup("Alto", "Alto", "Alto");
        evaluation.adjust(ana, level(defense, "Bajo"));
        IndividualGrade first = evaluation.getIndividualGrades().get(0);

        evaluation.adjust(ana, level(defense, "Básico"));

        assertThat(evaluation.getIndividualGrades()).containsExactly(first);
        assertThat(first.getLevel().getName()).isEqualTo("Básico");
        assertThat(first.getScore()).isEqualByComparingTo("0.48");
    }

    @Test
    void adjustingToTheGroupLevelRemovesTheAdjustment() {
        gradeGroup("Alto", "Alto", "Alto");
        evaluation.adjust(ana, level(defense, "Bajo"));

        evaluation.adjust(ana, level(defense, "Alto"));

        assertThat(evaluation.getIndividualGrades()).isEmpty();
        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("1.60");
    }

    @Test
    void changingTheGroupLevelToTheAdjustedOneDropsTheRedundantAdjustment() {
        gradeGroup("Alto", "Alto", "Alto");
        evaluation.adjust(ana, level(defense, "Sobresaliente"));
        evaluation.adjust(luis, level(defense, "Bajo"));

        evaluation.grade(level(defense, "Sobresaliente"));

        assertThat(evaluation.getAdjustmentsFor(ana)).isEmpty();
        assertThat(evaluation.getAdjustmentsFor(luis)).hasSize(1);
        assertThat(evaluation.getTotalScoreFor(luis)).isEqualByComparingTo("0.96");
    }

    @Test
    void anAdjustmentCountsEvenIfTheGroupHasNotGradedThatCriterionYet() {
        evaluation.grade(level(problem, "Alto"));

        evaluation.adjust(ana, level(defense, "Básico"));

        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("0.96");
        assertThat(evaluation.getScoreFor(rap5, ana)).isEqualByComparingTo("0.48");
        assertThat(evaluation.getTotalScoreFor(luis)).isEqualByComparingTo("0.48");
        assertThat(evaluation.isComplete()).isFalse();
    }

    @Test
    void removingAnAdjustmentGoesBackToTheGroupLevel() {
        gradeGroup("Alto", "Alto", "Alto");
        evaluation.adjust(ana, level(problem, "Bajo"));

        evaluation.removeAdjustment(ana, problem);
        evaluation.removeAdjustment(luis, problem);

        assertThat(evaluation.getIndividualGrades()).isEmpty();
        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("1.60");
    }

    @Test
    void onlyMembersOfTheGroupCanBeAdjusted() {
        assertThatThrownBy(() -> evaluation.adjust(new Student(), level(problem, "Alto")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAdjustmentKeepsTheScoreOfTheLevelAtTheTimeOfAdjusting() {
        gradeGroup("Alto", "Alto", "Alto");
        PerformanceLevel top = level(defense, "Sobresaliente");
        evaluation.adjust(ana, top);

        top.setScore(new BigDecimal("0.70"));

        assertThat(evaluation.getTotalScoreFor(ana)).isEqualByComparingTo("1.76");
    }

    private void gradeGroup(String problemLevel, String descriptionLevel, String defenseLevel) {
        evaluation.grade(level(problem, problemLevel));
        evaluation.grade(level(description, descriptionLevel));
        evaluation.grade(level(defense, defenseLevel));
    }

    /** Criterio con los cuatro niveles de la rúbrica, escalados a su puntaje máximo. */
    private static EvaluationCriterion criterion(String name, String max) {
        BigDecimal maxScore = new BigDecimal(max);
        EvaluationCriterion criterion = new EvaluationCriterion(name, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, BigDecimal.ZERO, 1));
        criterion.addLevel(new PerformanceLevel("Básico", null, maxScore.multiply(new BigDecimal("0.6")), 2));
        criterion.addLevel(new PerformanceLevel("Alto", null, maxScore.multiply(new BigDecimal("0.8")), 3));
        criterion.addLevel(new PerformanceLevel("Sobresaliente", null, maxScore, 4));
        return criterion;
    }

    private static PerformanceLevel level(EvaluationCriterion criterion, String name) {
        return criterion.getLevels().stream()
                .filter(level -> level.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
