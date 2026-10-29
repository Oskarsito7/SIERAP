package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.CopyMomentsRequest;
import co.edu.unicesar.sierap.dto.request.LaunchMomentRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Consulta y gestión de momentos (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 *
 * <p>Alcance de consulta: el super_admin ve todos; el admin y el docente, los de los
 * programas de su facultad; el estudiante, los de su programa en rango de semestre o
 * en los que ya tiene grupo.</p>
 *
 * <p>Alcance de gestión: super_admin y admin de la facultad del programa del momento.</p>
 */
public interface MomentService {

    /**
     * Lista los momentos de un periodo dentro del alcance del usuario.
     *
     * @param actor    usuario que consulta
     * @param periodId periodo académico; si es {@code null}, el periodo en curso (el
     *                 que incluye la fecha de hoy)
     * @return momentos; vacío si no hay un periodo en curso
     */
    List<MomentSummaryResponse> list(AuthenticatedUser actor, Long periodId);

    /**
     * Consulta un momento con su estructura de evaluación.
     *
     * @param actor usuario que consulta
     * @param id    momento
     * @return detalle del momento
     */
    MomentDetailResponse get(AuthenticatedUser actor, Long id);

    /**
     * Crea un nuevo momento en estado de configuración.
     *
     * @param actor   usuario que crea (super_admin o admin de la facultad del programa)
     * @param request datos del momento y sus ventanas opcionales
     * @return detalle del momento creado
     */
    MomentDetailResponse create(AuthenticatedUser actor, NewMomentRequest request);

    /**
     * Modifica parcialmente un momento en estado de configuración.
     *
     * @param actor   usuario que modifica (super_admin o admin de la facultad del programa)
     * @param id      identificador del momento
     * @param request campos a cambiar
     * @return detalle del momento modificado
     */
    MomentDetailResponse update(AuthenticatedUser actor, Long id, UpdateMomentRequest request);

    /**
     * Elimina un momento en estado de configuración sin grupos.
     *
     * @param actor usuario que elimina (super_admin o admin de la facultad del programa)
     * @param id    identificador del momento
     */
    void delete(AuthenticatedUser actor, Long id);

    /**
     * Reemplaza todos los pesos de evaluación del momento.
     *
     * @param actor   usuario que actualiza (super_admin o admin de la facultad del programa)
     * @param id      identificador del momento
     * @param request nuevos pesos
     * @return distribución de pesos actualizada
     */
    MomentWeightsResponse updateWeights(AuthenticatedUser actor, Long id, MomentWeightsRequest request);

    /**
     * Diagnostica la configuración de un momento antes del lanzamiento y lista
     * alertas (evaluadores sin peso y grupos por debajo del mínimo de integrantes).
     *
     * @param actor usuario que consulta
     * @param id    identificador del momento
     * @return diagnóstico de lanzamiento
     */
    LaunchCheckResponse launchCheck(AuthenticatedUser actor, Long id);

    /**
     * Lanza un momento a ejecución (pasa a estado lanzado).
     *
     * @param actor   usuario que lanza
     * @param id      identificador del momento
     * @param request confirmación opcional para lanzar con grupos por debajo del mínimo
     * @return detalle del momento lanzado
     */
    MomentDetailResponse launch(AuthenticatedUser actor, Long id, LaunchMomentRequest request);

    /**
     * Copia la configuración de momentos (RAP, criterios, niveles y pesos) de un
     * periodo académico a otro para los programas en alcance (ADR-010).
     *
     * @param actor   usuario que realiza la copia (super_admin o admin de facultad)
     * @param request parámetros de copia (periodo origen, destino y programa opcional)
     * @return lista de los nuevos momentos creados en el periodo destino
     */
    List<MomentSummaryResponse> copy(AuthenticatedUser actor, CopyMomentsRequest request);
}
