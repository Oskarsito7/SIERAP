package co.edu.unicesar.sierap.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.EvaluatorWeight;
import co.edu.unicesar.sierap.entity.Moment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la migración V9 contra PostgreSQL real
 * (Testcontainers): los pesos por tipo de evaluador del Momento 1 de la semilla y
 * las restricciones de la base de datos (ADR-018).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class EvaluatorWeightIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private EvaluatorTypeRepository evaluatorTypeRepository;

    private Moment moment;
    private EvaluatorType business;
    private EvaluatorType graduate;

    @BeforeEach
    void setUp() {
        moment = momentRepository.findByProgramCodeAndPeriodCodeAndNumber("108", "2026-2", 1).orElseThrow();
        business = evaluatorTypeRepository.findByName("Empresario").orElseThrow();
        graduate = evaluatorTypeRepository.findByName("Egresado").orElseThrow();
    }

    @Test
    void theSeedMomentKeepsTheTeacherAtOneHundredPercent() {
        assertThat(moment.getTeacherWeight()).isEqualByComparingTo("100");
        assertThat(moment.getEvaluatorWeights()).isEmpty();
    }

    @Test
    void savesAndReadsTheWeightOfTheTeacherAndOfEachType() {
        moment.setTeacherWeight(new BigDecimal("50"));
        moment.setEvaluatorWeight(business, new BigDecimal("33.33"));
        moment.setEvaluatorWeight(graduate, new BigDecimal("16.67"));
        flushAndClear();

        Moment loaded = momentRepository.findById(moment.getId()).orElseThrow();
        EvaluatorType loadedBusiness = evaluatorTypeRepository.findById(business.getId()).orElseThrow();

        assertThat(loaded.getTeacherWeight()).isEqualByComparingTo("50");
        assertThat(loaded.getWeightFor(loadedBusiness)).isEqualByComparingTo("33.33");
        assertThat(loaded.getEvaluatorWeights()).extracting(EvaluatorWeight::getWeight)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("33.33"), new BigDecimal("16.67"));
        assertThat(loaded.getTotalWeight()).isEqualByComparingTo("100");
    }

    @Test
    void removingAWeightDeletesItsRow() {
        moment.setEvaluatorWeight(business, new BigDecimal("30"));
        flushAndClear();

        Moment loaded = momentRepository.findById(moment.getId()).orElseThrow();
        loaded.removeEvaluatorWeight(evaluatorTypeRepository.findById(business.getId()).orElseThrow());
        flushAndClear();

        assertThat(countWeights()).isZero();
    }

    @Test
    void theDatabaseRejectsATeacherWeightOfZero() {
        assertThatThrownBy(() -> entityManager.createNativeQuery(
                        "UPDATE moment SET teacher_weight = 0 WHERE id = :id")
                .setParameter("id", moment.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void theDatabaseRejectsTheSameTypeTwiceInAMoment() {
        moment.setEvaluatorWeight(business, new BigDecimal("30"));
        flushAndClear();

        assertThatThrownBy(() -> entityManager.createNativeQuery(
                        "INSERT INTO evaluator_weight (moment_id, evaluator_type_id, weight) VALUES (:m, :t, 10)")
                .setParameter("m", moment.getId())
                .setParameter("t", business.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private long countWeights() {
        return ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM evaluator_weight WHERE moment_id = :id")
                .setParameter("id", moment.getId())
                .getSingleResult()).longValue();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
