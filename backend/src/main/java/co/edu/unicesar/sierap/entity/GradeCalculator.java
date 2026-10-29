package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fórmula de la nota de un momento (ADR-018, {@code docs/07}, "Results").
 *
 * <ol>
 *   <li>Solo cuentan las evaluaciones enviadas: quien llama pasa solo esas.</li>
 *   <li>Dentro de cada tipo de evaluador (el docente y cada {@code EvaluatorType}) se
 *       promedian los puntajes de sus evaluaciones.</li>
 *   <li>Los promedios se ponderan con los porcentajes del momento. Si un tipo no tiene
 *       evaluaciones enviadas, su peso se reparte entre los presentes en proporción a
 *       sus pesos: se divide entre la suma de los pesos presentes y no entre 100.</li>
 *   <li>Un tipo sin peso en el momento, o con peso 0, no cuenta (decisión del
 *       2026-10-16).</li>
 * </ol>
 *
 * <p>La misma fórmula sirve para la nota total y para la de cada RAP, y para la nota
 * del grupo y la individual: solo cambia el puntaje de cada evaluación que se le pasa.
 * La usa {@link ProjectGroup}, que le pasa sus evaluaciones enviadas y los pesos de su
 * {@link Moment}. Los pasos intermedios usan 16 dígitos significativos y solo el
 * resultado se redondea a 2 decimales, mitad hacia arriba (ADR-013, decisión del
 * 2026-10-16).</p>
 */
final class GradeCalculator {

    /** Decimales de las notas que devuelve la API ({@code docs/07}). */
    static final int SCALE = 2;

    /** Precisión de los promedios y de la ponderación antes de redondear. */
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private GradeCalculator() {
    }

    /**
     * Calcula la nota ponderada por tipo de evaluador.
     *
     * @param scores  puntaje de cada evaluación enviada, con el tipo de su evaluador
     * @param weights porcentaje de cada tipo de evaluador en el momento
     * @return la nota con 2 decimales, o vacío si ninguna evaluación tiene peso
     */
    static Optional<BigDecimal> weightedGrade(List<KindScore> scores, Map<EvaluatorKind, BigDecimal> weights) {
        Map<EvaluatorKind, List<BigDecimal>> scoresByKind = new LinkedHashMap<>();
        for (KindScore score : scores) {
            scoresByKind.computeIfAbsent(score.kind(), kind -> new ArrayList<>()).add(score.score());
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal presentWeight = BigDecimal.ZERO;
        for (Map.Entry<EvaluatorKind, List<BigDecimal>> entry : scoresByKind.entrySet()) {
            BigDecimal weight = weights.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (weight.signum() <= 0) {
                continue;
            }
            weightedSum = weightedSum.add(weight.multiply(average(entry.getValue()), PRECISION));
            presentWeight = presentWeight.add(weight);
        }

        if (presentWeight.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(weightedSum.divide(presentWeight, PRECISION).setScale(SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), PRECISION);
    }

    /**
     * Tipo de evaluador al que se le asigna un peso: el docente del grupo o un tipo del
     * catálogo {@code EvaluatorType}.
     *
     * @param evaluatorTypeId id del tipo de evaluador; {@code null} para el docente
     */
    record EvaluatorKind(Long evaluatorTypeId) {

        /** El docente que habilitó el grupo. */
        static final EvaluatorKind TEACHER = new EvaluatorKind(null);

        /**
         * Tipo de evaluador externo del catálogo.
         *
         * @param evaluatorTypeId id del {@code EvaluatorType}
         * @return el tipo correspondiente
         */
        static EvaluatorKind ofType(Long evaluatorTypeId) {
            return new EvaluatorKind(Objects.requireNonNull(evaluatorTypeId, "evaluatorTypeId"));
        }
    }

    /**
     * Puntaje de una evaluación enviada junto con el tipo de su evaluador.
     *
     * @param kind  tipo de evaluador que la hizo
     * @param score puntaje de la evaluación (total o de un RAP, del grupo o de un estudiante)
     */
    record KindScore(EvaluatorKind kind, BigDecimal score) {

        KindScore {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(score, "score");
        }
    }
}
