package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
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
     * Evaluaciones de un grupo en un estado; con {@code SUBMITTED} se obtienen las
     * que cuentan para la nota del grupo.
     *
     * @param projectGroupId id del grupo
     * @param status         estado buscado
     * @return evaluaciones del grupo en ese estado
     */
    List<Evaluation> findByProjectGroupIdAndStatus(Long projectGroupId, EvaluationStatus status);

    /**
     * Evaluaciones de todos los grupos de un momento en un estado, para los reportes.
     *
     * @param momentId id del momento
     * @param status   estado buscado (normalmente {@code SUBMITTED})
     * @return evaluaciones del momento en ese estado
     */
    List<Evaluation> findByProjectGroupMomentIdAndStatus(Long momentId, EvaluationStatus status);

    /**
     * Indica si un evaluador ya tiene una evaluación (borrador o enviada) del grupo.
     *
     * @param projectGroupId id del grupo
     * @param evaluatorId    id del usuario evaluador
     * @return {@code true} si ya empezó a calificar
     */
    boolean existsByProjectGroupIdAndEvaluatorId(Long projectGroupId, Long evaluatorId);

    /**
     * Indica si el grupo tiene alguna evaluación en el estado indicado.
     *
     * @param projectGroupId id del grupo
     * @param status         estado buscado
     * @return {@code true} si hay al menos una
     */
    boolean existsByProjectGroupIdAndStatus(Long projectGroupId, EvaluationStatus status);
}
