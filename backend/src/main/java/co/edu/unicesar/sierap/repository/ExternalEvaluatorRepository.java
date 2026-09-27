package co.edu.unicesar.sierap.repository;

import java.util.List;

import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link ExternalEvaluator}.
 */
public interface ExternalEvaluatorRepository extends JpaRepository<ExternalEvaluator, Long> {

    /**
     * Lista los evaluadores externos de un tipo del catálogo.
     *
     * @param evaluatorTypeId identificador del tipo de evaluador
     * @return evaluadores externos de ese tipo
     */
    List<ExternalEvaluator> findByEvaluatorTypeId(Long evaluatorTypeId);

    /**
     * Lista los evaluadores externos que creó un usuario.
     *
     * @param createdById identificador de quien los creó
     * @return evaluadores externos creados por ese usuario
     */
    List<ExternalEvaluator> findByCreatedByIdOrderByLastNamesAsc(Long createdById);
}
