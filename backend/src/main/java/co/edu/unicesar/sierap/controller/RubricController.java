package co.edu.unicesar.sierap.controller;

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
import co.edu.unicesar.sierap.service.RubricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para configurar la estructura de evaluación (RAP, criterios y niveles)
 * de un momento en configuración (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class RubricController {

    private final RubricService rubricService;

    /**
     * Crea el controlador con el servicio de rúbrica.
     *
     * @param rubricService servicio de rúbrica
     */
    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    /**
     * Registra un RAP en un momento en configuración.
     *
     * @param actor    usuario autenticado
     * @param momentId identificador del momento
     * @param request  datos del nuevo RAP
     * @return RAP creado
     */
    @PostMapping("/moments/{momentId}/learning-outcomes")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningOutcomeResponse createLearningOutcome(@AuthenticationPrincipal AuthenticatedUser actor,
                                                         @PathVariable Long momentId,
                                                         @Valid @RequestBody NewLearningOutcomeRequest request) {
        return rubricService.createLearningOutcome(actor, momentId, request);
    }

    /**
     * Modifica parcialmente un RAP.
     *
     * @param actor   usuario autenticado
     * @param id      identificador del RAP
     * @param request campos a cambiar
     * @return RAP actualizado
     */
    @PatchMapping("/learning-outcomes/{id}")
    public LearningOutcomeResponse updateLearningOutcome(@AuthenticationPrincipal AuthenticatedUser actor,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody UpdateLearningOutcomeRequest request) {
        return rubricService.updateLearningOutcome(actor, id, request);
    }

    /**
     * Elimina un RAP con todos sus criterios y niveles.
     *
     * @param actor usuario autenticado
     * @param id    identificador del RAP
     */
    @DeleteMapping("/learning-outcomes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLearningOutcome(@AuthenticationPrincipal AuthenticatedUser actor,
                                      @PathVariable Long id) {
        rubricService.deleteLearningOutcome(actor, id);
    }

    /**
     * Registra un criterio de evaluación en un RAP.
     *
     * @param actor     usuario autenticado
     * @param outcomeId identificador del RAP
     * @param request   datos del nuevo criterio
     * @return criterio creado
     */
    @PostMapping("/learning-outcomes/{outcomeId}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public CriterionResponse createCriterion(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @PathVariable Long outcomeId,
                                            @Valid @RequestBody NewCriterionRequest request) {
        return rubricService.createCriterion(actor, outcomeId, request);
    }

    /**
     * Modifica parcialmente un criterio de evaluación.
     *
     * @param actor   usuario autenticado
     * @param id      identificador del criterio
     * @param request campos a cambiar
     * @return criterio actualizado
     */
    @PatchMapping("/criteria/{id}")
    public CriterionResponse updateCriterion(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @PathVariable Long id,
                                            @Valid @RequestBody UpdateCriterionRequest request) {
        return rubricService.updateCriterion(actor, id, request);
    }

    /**
     * Elimina un criterio con todos sus niveles de desempeño.
     *
     * @param actor usuario autenticado
     * @param id    identificador del criterio
     */
    @DeleteMapping("/criteria/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCriterion(@AuthenticationPrincipal AuthenticatedUser actor,
                                @PathVariable Long id) {
        rubricService.deleteCriterion(actor, id);
    }

    /**
     * Registra un nivel de desempeño en un criterio de evaluación.
     *
     * @param actor       usuario autenticado
     * @param criterionId identificador del criterio
     * @param request     datos del nuevo nivel
     * @return nivel creado
     */
    @PostMapping("/criteria/{criterionId}/levels")
    @ResponseStatus(HttpStatus.CREATED)
    public LevelResponse createLevel(@AuthenticationPrincipal AuthenticatedUser actor,
                                     @PathVariable Long criterionId,
                                     @Valid @RequestBody NewLevelRequest request) {
        return rubricService.createLevel(actor, criterionId, request);
    }

    /**
     * Modifica parcialmente un nivel de desempeño.
     *
     * @param actor   usuario autenticado
     * @param id      identificador del nivel
     * @param request campos a cambiar
     * @return nivel actualizado
     */
    @PatchMapping("/levels/{id}")
    public LevelResponse updateLevel(@AuthenticationPrincipal AuthenticatedUser actor,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateLevelRequest request) {
        return rubricService.updateLevel(actor, id, request);
    }

    /**
     * Elimina un nivel de desempeño de un criterio.
     *
     * @param actor usuario autenticado
     * @param id    identificador del nivel
     */
    @DeleteMapping("/levels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLevel(@AuthenticationPrincipal AuthenticatedUser actor,
                            @PathVariable Long id) {
        rubricService.deleteLevel(actor, id);
    }
}
