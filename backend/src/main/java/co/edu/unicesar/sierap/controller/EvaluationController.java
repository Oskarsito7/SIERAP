package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.SaveEvaluationRequest;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse;
import co.edu.unicesar.sierap.dto.response.EvaluationSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Calificación de los grupos (Etapa 6, {@code docs/07} "Evaluation"): la evaluación
 * propia de cada evaluador y la lista de evaluaciones del grupo.
 *
 * <p>Aquí solo se traduce HTTP: quién califica, cuándo y con qué niveles lo valida
 * {@link EvaluationService}.</p>
 */
@RestController
@RequestMapping("/api/groups/{id}")
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * Crea el controller.
     *
     * @param evaluationService service de evaluaciones
     */
    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Evaluación propia del grupo, o una vacía si todavía no se empezó.
     *
     * @param actor docente del grupo o evaluador externo asignado
     * @param id    grupo
     * @return la evaluación con la estructura del momento
     */
    @GetMapping("/evaluation")
    @PreAuthorize("hasAnyRole('TEACHER', 'EXTERNAL_EVALUATOR')")
    public EvaluationFormResponse form(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return evaluationService.form(actor, id);
    }

    /**
     * Guarda la evaluación propia como borrador (o la corrige si ya fue enviada).
     *
     * @param actor   docente del grupo o evaluador externo asignado
     * @param id      grupo
     * @param request niveles, observación y ajustes individuales
     * @return la evaluación actualizada
     */
    @PutMapping("/evaluation")
    @PreAuthorize("hasAnyRole('TEACHER', 'EXTERNAL_EVALUATOR')")
    public EvaluationFormResponse save(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                       @Valid @RequestBody SaveEvaluationRequest request) {
        return evaluationService.save(actor, id, request);
    }

    /**
     * Envía la evaluación propia; desde ahí cuenta para la nota.
     *
     * @param actor docente del grupo o evaluador externo asignado
     * @param id    grupo
     * @return la evaluación enviada
     */
    @PostMapping("/evaluation/submit")
    @PreAuthorize("hasAnyRole('TEACHER', 'EXTERNAL_EVALUATOR')")
    public EvaluationFormResponse submit(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return evaluationService.submit(actor, id);
    }

    /**
     * Evaluaciones del grupo.
     *
     * @param actor docente del grupo, evaluador externo (solo la suya), admin de la
     *              facultad o super_admin
     * @param id    grupo
     * @return evaluaciones, la del docente primero
     */
    @GetMapping("/evaluations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR')")
    public List<EvaluationSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                                @PathVariable Long id) {
        return evaluationService.list(actor, id);
    }
}
