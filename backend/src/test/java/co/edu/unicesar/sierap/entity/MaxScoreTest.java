package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link EvaluationCriterion#getMaxScore()} y
 * {@link LearningOutcome#getMaxScore()}.
 */
class MaxScoreTest {

    @Test
    void criterionTakesTheScoreOfTheHighestLevelRegardlessOfOrder() {
        EvaluationCriterion criterion = new EvaluationCriterion("Identificación del problema", 1);
        criterion.addLevel(level("Alto", "0.48", 3));
        criterion.addLevel(level("Sobresaliente", "0.60", 4));
        criterion.addLevel(level("Bajo", "0", 1));
        criterion.addLevel(level("Básico", "0.36", 2));

        assertThat(criterion.getMaxScore()).isEqualByComparingTo("0.60");
    }

    @Test
    void criterionWithoutLevelsIsWorthZero() {
        assertThat(new EvaluationCriterion("Sin niveles", 1).getMaxScore())
                .isEqualByComparingTo("0");
    }

    @Test
    void learningOutcomeAddsUpTheMaximumOfEachCriterion() {
        EvaluationCriterion description = new EvaluationCriterion("Descripción de la solución", 1);
        description.addLevel(level("Bajo", "0", 1));
        description.addLevel(level("Sobresaliente", "0.60", 2));
        EvaluationCriterion defense = new EvaluationCriterion("Sustentación", 2);
        defense.addLevel(level("Bajo", "0", 1));
        defense.addLevel(level("Sobresaliente", "0.80", 2));
        LearningOutcome outcome = new LearningOutcome("RAP5", "EMPRENDE...");
        outcome.addCriterion(description);
        outcome.addCriterion(defense);

        assertThat(outcome.getMaxScore()).isEqualByComparingTo("1.40");
    }

    @Test
    void learningOutcomeWithoutCriteriaIsWorthZero() {
        assertThat(new LearningOutcome("RAP1", "Sin criterios").getMaxScore()).isEqualByComparingTo("0");
    }

    @Test
    void removingACriterionTakesItOutOfTheLearningOutcome() {
        EvaluationCriterion criterion = new EvaluationCriterion("Único", 1);
        criterion.addLevel(level("Sobresaliente", "0.60", 1));
        LearningOutcome outcome = new LearningOutcome("RAP1", "Descripción");
        outcome.addCriterion(criterion);

        outcome.removeCriterion(criterion);

        assertThat(outcome.getMaxScore()).isEqualByComparingTo("0");
        assertThat(criterion.getLearningOutcome()).isNull();
    }

    private static PerformanceLevel level(String name, String score, int displayOrder) {
        return new PerformanceLevel(name, null, new BigDecimal(score), displayOrder);
    }
}
