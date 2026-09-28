package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Consulta de momentos (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 *
 * <p>Alcance: el super_admin ve todos; el admin y el docente, los de los programas de
 * su facultad; el estudiante, los de su programa cuyo rango de semestres incluye su
 * semestre actual.</p>
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
}
