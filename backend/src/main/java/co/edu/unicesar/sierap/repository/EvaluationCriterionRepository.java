package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link EvaluationCriterion}.
 */
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
}
