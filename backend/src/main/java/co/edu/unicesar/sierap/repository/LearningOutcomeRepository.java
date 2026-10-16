package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.LearningOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link LearningOutcome}.
 */
public interface LearningOutcomeRepository extends JpaRepository<LearningOutcome, Long> {

    /**
     * Indica si ya existe un RAP con ese código en el momento.
     *
     * @param momentId id del momento
     * @param code     código del RAP
     * @return {@code true} si ya existe
     */
    boolean existsByMomentIdAndCode(Long momentId, String code);

    /**
     * Indica si ya existe otro RAP diferente con ese código en el momento.
     *
     * @param momentId id del momento
     * @param code     código del RAP
     * @param id       id del RAP a excluir
     * @return {@code true} si otro RAP ya tiene ese código
     */
    boolean existsByMomentIdAndCodeAndIdNot(Long momentId, String code, Long id);
}
