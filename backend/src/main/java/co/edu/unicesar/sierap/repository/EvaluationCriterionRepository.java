package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link EvaluationCriterion}.
 */
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
}
