package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de las notas de {@link ProjectGroup} (ADR-018): la del grupo y la
 * individual, en total y por RAP, a partir de las evaluaciones enviadas y los pesos del
 * momento (docente 60 %, empresario 20 %, egresado 20 %).
 */
class ProjectGroupGradeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 8, 0);

    private LearningOutcome rap1;
    private LearningOutcome rap2;
    private EvaluationCriterion firstCriterion;
    private EvaluationCriterion secondCriterion;
    private EvaluatorType businessType;
    private Teacher teacher;
    private ProjectGroup group;
    private Student ana;
    private Student beto;

    @BeforeEach
    void setUp() {
        firstCriterion = criterion("Criterio 1", "0.00", "1.00");
        secondCriterion = criterion("Criterio 2", "0.50", "2.00");
        rap1 = new LearningOutcome("RAP1", "Primer RAP");
        rap1.addCriterion(firstCriterion);
        rap2 = new LearningOutcome("RAP2", "Segundo RAP");
        rap2.addCriterion(secondCriterion);

        businessType = evaluatorType(1L, "Empresario");
        Moment moment = new Moment();
        moment.addLearningOutcome(rap1);
        moment.addLearningOutcome(rap2);
        moment.setTeacherWeight(new BigDecimal("60"));
        moment.setEvaluatorWeight(businessType, new BigDecimal("20"));
        moment.setEvaluatorWeight(evaluatorType(2L, "Egresado"), new BigDecimal("20"));

        teacher = new Teacher();
        group = new ProjectGroup(moment, teacher, "Grupo 1");
        ana = new Student();
        beto = new Student();
        group.addMember(ana);
        group.addMember(beto);
    }

    @Test
    void hasNoGradeWithoutSubmittedEvaluations() {
        evaluation(teacher, "1.00", "2.00");

        assertThat(group.getSubmittedEvaluations()).isEmpty();
        assertThat(group.getGroupGrade()).isEmpty();
        assertThat(group.getIndividualGrade(ana)).isEmpty();
    }

    @Test
    void weightsTheKindsAndSpreadsTheWeightOfTheAbsentOnes() {
        evaluation(teacher, "1.00", "2.00").submit();
        evaluation(externalEvaluator(businessType), "0.00", "2.00").submit();

        // Sin egresado, 60/20 pasa a 75/25: 0,75 × 3,00 + 0,25 × 2,00 = 2,75
        assertThat(group.getGroupGrade()).hasValueSatisfying(grade -> assertThat(grade).isEqualByComparingTo("2.75"));
        assertThat(group.getGroupScoreFor(rap1)).hasValueSatisfying(score -> assertThat(score).isEqualByComparingTo("0.75"));
        assertThat(group.getGroupScoreFor(rap2)).hasValueSatisfying(score -> assertThat(score).isEqualByComparingTo("2.00"));
    }

    @Test
    void theIndividualGradeUsesTheStudentAdjustments() {
        Evaluation byTeacher = evaluation(teacher, "1.00", "2.00");
        byTeacher.adjust(ana, level(secondCriterion, "0.50"));
        byTeacher.submit();
        evaluation(externalEvaluator(businessType), "0.00", "2.00").submit();

        // Ana: 0,75 × 1,50 + 0,25 × 2,00 = 1,625 → 1,63; Beto hereda la nota del grupo
        assertThat(group.getIndividualGrade(ana)).hasValueSatisfying(grade -> assertThat(grade).isEqualByComparingTo("1.63"));
        assertThat(group.getIndividualScoreFor(rap2, ana))
                .hasValueSatisfying(score -> assertThat(score).isEqualByComparingTo("0.88"));
        assertThat(group.getIndividualGrade(beto)).hasValueSatisfying(grade -> assertThat(grade).isEqualByComparingTo("2.75"));
    }

    @Test
    void anEvaluatorTypeWithoutWeightDoesNotCount() {
        evaluation(teacher, "1.00", "2.00").submit();
        evaluation(externalEvaluator(evaluatorType(3L, "Jurado")), "0.00", "0.50").submit();

        assertThat(group.getGroupGrade()).hasValueSatisfying(grade -> assertThat(grade).isEqualByComparingTo("3.00"));
    }

    /** Evaluación en borrador del evaluador, con el nivel de puntaje dado en cada criterio. */
    private Evaluation evaluation(User evaluator, String firstScore, String secondScore) {
        Evaluation evaluation = new Evaluation(evaluator, NOW);
        group.addEvaluation(evaluation);
        evaluation.grade(level(firstCriterion, firstScore));
        evaluation.grade(level(secondCriterion, secondScore));
        return evaluation;
    }

    /** Evaluador externo del tipo dado, asignado al grupo. */
    private ExternalEvaluator externalEvaluator(EvaluatorType type) {
        ExternalEvaluator evaluator = new ExternalEvaluator("Evaluador", type.getName(), DocumentType.CC, "1",
                type.getName().toLowerCase() + "@correo.com", "hash", NOW, type);
        group.addEvaluator(evaluator);
        return evaluator;
    }

    /** Tipo de evaluador con id, como si ya estuviera guardado: los pesos se buscan por id. */
    private static EvaluatorType evaluatorType(Long id, String name) {
        EvaluatorType type = new EvaluatorType(name, null);
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }

    /** Criterio con dos niveles: el bajo y el alto, con sus puntajes. */
    private static EvaluationCriterion criterion(String name, String low, String high) {
        EvaluationCriterion criterion = new EvaluationCriterion(name, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, new BigDecimal(low), 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(high), 2));
        return criterion;
    }

    private static PerformanceLevel level(EvaluationCriterion criterion, String score) {
        return criterion.getLevels().stream()
                .filter(level -> level.getScore().compareTo(new BigDecimal(score)) == 0)
                .findFirst()
                .orElseThrow();
    }
}
