package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.GradeCalculator.EvaluatorKind;
import co.edu.unicesar.sierap.entity.GradeCalculator.KindScore;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de la fórmula de la nota (ADR-018): promedio dentro de cada tipo
 * de evaluador, ponderación con los porcentajes del momento, reparto del peso de los
 * tipos ausentes y redondeo final a 2 decimales.
 */
class GradeCalculatorTest {

    private static final EvaluatorKind BUSINESS = EvaluatorKind.ofType(1L);
    private static final EvaluatorKind ALUMNUS = EvaluatorKind.ofType(2L);

    /** Pesos del ejemplo de los wireframes (AD-04): docente 60 %, empresario y egresado 20 %. */
    private static final Map<EvaluatorKind, BigDecimal> WEIGHTS = Map.of(
            EvaluatorKind.TEACHER, new BigDecimal("60"),
            BUSINESS, new BigDecimal("20"),
            ALUMNUS, new BigDecimal("20"));

    @Test
    void weightsEachKindWithItsPercentage() {
        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "4.50"),
                score(BUSINESS, "4.00"),
                score(ALUMNUS, "3.50")), WEIGHTS);

        // 0,6 × 4,50 + 0,2 × 4,00 + 0,2 × 3,50 = 4,20
        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("4.20"));
    }

    @Test
    void averagesTheEvaluatorsOfTheSameKind() {
        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "4.50"),
                score(BUSINESS, "4.00"),
                score(BUSINESS, "3.00"),
                score(ALUMNUS, "3.50")), WEIGHTS);

        // Empresarios: (4,00 + 3,00) / 2 = 3,50 → 0,6 × 4,50 + 0,2 × 3,50 + 0,2 × 3,50 = 4,10
        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("4.10"));
    }

    @Test
    void redistributesTheWeightOfAKindWithoutSubmittedEvaluations() {
        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "4.50"),
                score(BUSINESS, "4.00")), WEIGHTS);

        // Sin egresado, 60/20 pasa a 75/25: 0,75 × 4,50 + 0,25 × 4,00 = 4,375 → 4,38
        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("4.38"));
    }

    @Test
    void ignoresAKindWithoutWeightInTheMoment() {
        EvaluatorKind otherType = EvaluatorKind.ofType(99L);

        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "4.00"),
                score(otherType, "1.00")), WEIGHTS);

        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("4.00"));
    }

    @Test
    void ignoresAKindWhoseWeightIsZero() {
        Map<EvaluatorKind, BigDecimal> weights = Map.of(
                EvaluatorKind.TEACHER, new BigDecimal("100"),
                BUSINESS, BigDecimal.ZERO);

        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "3.80"),
                score(BUSINESS, "1.00")), weights);

        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("3.80"));
    }

    @Test
    void hasNoGradeWithoutSubmittedEvaluations() {
        assertThat(GradeCalculator.weightedGrade(List.of(), WEIGHTS)).isEmpty();
    }

    @Test
    void hasNoGradeWhenNoEvaluationHasWeight() {
        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.ofType(99L), "4.00")), WEIGHTS);

        assertThat(grade).isEmpty();
    }

    @Test
    void roundsHalfUpOnlyAtTheEnd() {
        Map<EvaluatorKind, BigDecimal> teacherOnly = Map.of(EvaluatorKind.TEACHER, new BigDecimal("100"));

        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "2.99"),
                score(EvaluatorKind.TEACHER, "3.00")), teacherOnly);

        // (2,99 + 3,00) / 2 = 2,995 → 3,00 (mitad hacia arriba, no 2,99)
        assertThat(grade).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("3.00"));
    }

    @Test
    void alwaysAnswersWithTwoDecimals() {
        Optional<BigDecimal> grade = GradeCalculator.weightedGrade(List.of(
                score(EvaluatorKind.TEACHER, "4.5"),
                score(BUSINESS, "4"),
                score(ALUMNUS, "3.5")), WEIGHTS);

        assertThat(grade).hasValueSatisfying(value -> assertThat(value.scale()).isEqualTo(GradeCalculator.SCALE));
    }

    private static KindScore score(EvaluatorKind kind, String value) {
        return new KindScore(kind, new BigDecimal(value));
    }
}
