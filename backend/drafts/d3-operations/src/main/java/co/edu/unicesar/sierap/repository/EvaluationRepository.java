package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Evaluation}.
 */
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /**
     * Evaluación que un evaluador le hizo a un grupo (hay como máximo una).
     *
     * @param projectGroupId id del grupo
     * @param evaluatorId    id del usuario evaluador
     * @return la evaluación, si ya calificó
     */
    Optional<Evaluation> findByProjectGroupIdAndEvaluatorId(Long projectGroupId, Long evaluatorId);

    /**
     * Todas las evaluaciones de un grupo (docente y evaluadores externos).
     *
     * @param projectGroupId id del grupo
     * @return evaluaciones recibidas
     */
    List<Evaluation> findByProjectGroupId(Long projectGroupId);

    /**
     * Evaluaciones de todos los grupos de un momento, para los reportes.
     *
     * @param momentId id del momento
     * @return evaluaciones del momento
     */
    List<Evaluation> findByProjectGroupMomentId(Long momentId);
}
