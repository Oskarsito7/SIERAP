package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.SaveEvaluationRequest;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse;
import co.edu.unicesar.sierap.dto.response.EvaluationSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Calificación de los grupos (Etapa 6, {@code docs/07} "Evaluation"). Cada evaluador
 * del grupo (su docente y los evaluadores externos asignados) tiene una sola
 * evaluación: califica el grupo una vez y puede ajustar el nivel de algunos criterios
 * para un estudiante (ADR-018). Solo se califica con el momento lanzado y entre la
 * fecha de evaluación del grupo y el fin de la ventana de calificación, según el
 * {@code Clock} inyectado.
 */
public interface EvaluationService {

    /**
     * Evaluación propia del grupo, o una vacía si todavía no se empezó.
     *
     * @param actor   docente del grupo o evaluador externo asignado
     * @param groupId grupo
     * @return la evaluación con la estructura del momento
     */
    EvaluationFormResponse form(AuthenticatedUser actor, Long groupId);

    /**
     * Guarda la evaluación propia: la crea como borrador la primera vez (puede quedar
     * a medias), cambia los niveles enviados, la observación y, si se envían, reemplaza
     * los ajustes individuales.
     *
     * @param actor   docente del grupo o evaluador externo asignado
     * @param groupId grupo
     * @param request cambios
     * @return la evaluación actualizada
     */
    EvaluationFormResponse save(AuthenticatedUser actor, Long groupId, SaveEvaluationRequest request);

    /**
     * Envía la evaluación propia: desde ahí cuenta para la nota. Debe estar completa.
     *
     * @param actor   docente del grupo o evaluador externo asignado
     * @param groupId grupo
     * @return la evaluación enviada
     */
    EvaluationFormResponse submit(AuthenticatedUser actor, Long groupId);

    /**
     * Evaluaciones del grupo: todas para su docente, el admin de la facultad y el
     * super_admin; solo la propia para un evaluador externo.
     *
     * @param actor   usuario que consulta
     * @param groupId grupo
     * @return evaluaciones, la del docente primero
     */
    List<EvaluationSummaryResponse> list(AuthenticatedUser actor, Long groupId);
}
