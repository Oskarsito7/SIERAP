package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.request.NewCriterionRequest;
import co.edu.unicesar.sierap.dto.request.NewLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.NewLevelRequest;
import co.edu.unicesar.sierap.dto.request.UpdateCriterionRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLevelRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LearningOutcomeResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Gestión de la estructura de evaluación (RAP, criterios y niveles de desempeño)
 * de un momento en configuración (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 */
public interface RubricService {

    /**
     * Registra un nuevo RAP en un momento en configuración.
     *
     * @param actor    usuario que actúa
     * @param momentId identificador del momento
     * @param request  datos del nuevo RAP
     * @return RAP registrado
     */
    LearningOutcomeResponse createLearningOutcome(AuthenticatedUser actor, Long momentId, NewLearningOutcomeRequest request);

    /**
     * Modifica parcialmente un RAP de un momento en configuración.
     *
     * @param actor   usuario que actúa
     * @param id      identificador del RAP
     * @param request campos a modificar
     * @return RAP actualizado
     */
    LearningOutcomeResponse updateLearningOutcome(AuthenticatedUser actor, Long id, UpdateLearningOutcomeRequest request);

    /**
     * Elimina un RAP con todos sus criterios y niveles de un momento en configuración.
     *
     * @param actor usuario que actúa
     * @param id    identificador del RAP
     */
    void deleteLearningOutcome(AuthenticatedUser actor, Long id);

    /**
     * Registra un nuevo criterio de evaluación en un RAP.
     *
     * @param actor     usuario que actúa
     * @param outcomeId identificador del RAP
     * @param request   datos del nuevo criterio
     * @return criterio registrado
     */
    CriterionResponse createCriterion(AuthenticatedUser actor, Long outcomeId, NewCriterionRequest request);

    /**
     * Modifica parcialmente un criterio de evaluación.
     *
     * @param actor   usuario que actúa
     * @param id      identificador del criterio
     * @param request campos a modificar
     * @return criterio actualizado
     */
    CriterionResponse updateCriterion(AuthenticatedUser actor, Long id, UpdateCriterionRequest request);

    /**
     * Elimina un criterio con todos sus niveles de desempeño.
     *
     * @param actor usuario que actúa
     * @param id    identificador del criterio
     */
    void deleteCriterion(AuthenticatedUser actor, Long id);

    /**
     * Registra un nuevo nivel de desempeño en un criterio de evaluación.
     *
     * @param actor       usuario que actúa
     * @param criterionId identificador del criterio
     * @param request     datos del nuevo nivel
     * @return nivel registrado
     */
    LevelResponse createLevel(AuthenticatedUser actor, Long criterionId, NewLevelRequest request);

    /**
     * Modifica parcialmente un nivel de desempeño.
     *
     * @param actor   usuario que actúa
     * @param id      identificador del nivel
     * @param request campos a modificar
     * @return nivel actualizado
     */
    LevelResponse updateLevel(AuthenticatedUser actor, Long id, UpdateLevelRequest request);

    /**
     * Elimina un nivel de desempeño de un criterio.
     *
     * @param actor usuario que actúa
     * @param id    identificador del nivel
     */
    void deleteLevel(AuthenticatedUser actor, Long id);
}
