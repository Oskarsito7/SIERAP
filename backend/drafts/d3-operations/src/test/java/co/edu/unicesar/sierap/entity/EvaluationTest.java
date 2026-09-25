package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del cálculo de notas de {@link Evaluation} y de
 * {@link CriterionGrade}: suma por RAP y total, corrección de un criterio y
 * evaluación completa.
 */
class EvaluationTest {

    private LearningOutcome rap1;
    private LearningOutcome rap5;
    private EvaluationCriterion problem;
    private EvaluationCriterion description;
    private EvaluationCriterion defense;
    private Evaluation evaluation;

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
    void rejectsALevelOfAnotherCriterion() {
        evaluation.grade(level(problem, "Alto"));
        CriterionGrade grade = evaluation.getGrades().get(0);

        assertThatThrownBy(() -> grade.changeLevel(level(defense, "Alto")))
                .isInstanceOf(IllegalArgumentException.class);
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
