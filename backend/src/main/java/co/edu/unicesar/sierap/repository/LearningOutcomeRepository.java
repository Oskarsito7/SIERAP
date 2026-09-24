package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.LearningOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link LearningOutcome}.
 */
public interface LearningOutcomeRepository extends JpaRepository<LearningOutcome, Long> {
}
