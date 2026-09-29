package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.AddMemberRequest;
import co.edu.unicesar.sierap.dto.request.AssignEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewGroupRequest;
import co.edu.unicesar.sierap.dto.request.ProjectTitleRequest;
import co.edu.unicesar.sierap.dto.request.UpdateGroupRequest;
import co.edu.unicesar.sierap.dto.response.GroupResponse;
import co.edu.unicesar.sierap.dto.response.StudentWithoutGroupResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Operación de los grupos de un momento (Etapa 4, {@code docs/07} "Groups"): el docente
 * habilita sus grupos, fija sus fechas, agrega estudiantes sin grupo y asigna
 * evaluadores externos. Cada operación valida el alcance del usuario y el estado y las
 * ventanas del momento (ADR-012) con el {@code Clock} inyectado.
 */
public interface GroupService {

    /**
     * Lista los grupos dentro del alcance del usuario.
     *
     * @param actor    docente (sus grupos), evaluador externo (los asignados), admin
     *                 (su facultad) o super_admin
     * @param momentId momento; opcional para docentes y evaluadores, obligatorio para
     *                 admin y super_admin
     * @return grupos ordenados por nombre
     */
    List<GroupResponse> list(AuthenticatedUser actor, Long momentId);

    /**
     * Consulta un grupo.
     *
     * @param actor usuario que consulta
     * @param id    grupo
     * @return el grupo
     */
    GroupResponse get(AuthenticatedUser actor, Long id);

    /**
     * Habilita un grupo del docente en un momento de su facultad que sigue en
     * configuración.
     *
     * @param actor   docente
     * @param request momento y nombre
     * @return grupo creado
     */
    GroupResponse create(AuthenticatedUser actor, NewGroupRequest request);

    /**
     * Cambia el nombre (solo antes del lanzamiento) o las fechas del grupo (dentro de
     * las ventanas del momento y nunca antes de hoy).
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request campos a cambiar
     * @return grupo actualizado
     */
    GroupResponse update(AuthenticatedUser actor, Long id, UpdateGroupRequest request);

    /**
     * Elimina un grupo sin integrantes antes del lanzamiento.
     *
     * @param actor docente del grupo
     * @param id    grupo
     */
    void delete(AuthenticatedUser actor, Long id);

    /**
     * Cambia el título del proyecto hasta que termine la ventana de entregas.
     *
     * @param actor   docente del grupo o uno de sus integrantes
     * @param id      grupo
     * @param request título
     * @return grupo actualizado
     */
    GroupResponse updateProjectTitle(AuthenticatedUser actor, Long id, ProjectTitleRequest request);

    /**
     * Lista los estudiantes del programa del momento, con su semestre en el rango del
     * momento, que no tienen grupo en él.
     *
     * @param actor    docente o admin de la facultad
     * @param momentId momento
     * @return estudiantes sin grupo
     */
    List<StudentWithoutGroupResponse> studentsWithoutGroup(AuthenticatedUser actor, Long momentId);

    /**
     * Agrega a uno de sus grupos a un estudiante del programa del momento que no tiene
     * grupo en él, aunque esté fuera del rango de semestres, después del lanzamiento o
     * por encima del máximo; hasta que cierre la ventana de calificación.
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request estudiante
     * @return grupo actualizado
     */
    GroupResponse addMember(AuthenticatedUser actor, Long id, AddMemberRequest request);

    /**
     * Retira a un integrante mientras el grupo no tenga evaluaciones enviadas.
     *
     * @param actor     docente del grupo
     * @param id        grupo
     * @param studentId integrante
     */
    void removeMember(AuthenticatedUser actor, Long id, Long studentId);

    /**
     * Busca un evaluador externo activo por coincidencia exacta de correo o documento,
     * para reutilizarlo aunque lo haya creado otro docente.
     *
     * @param query correo o número de documento exacto
     * @return el evaluador
     */
    UserSummaryResponse lookupExternalEvaluator(String query);

    /**
     * Asigna un evaluador externo activo a uno de sus grupos, hasta que cierre la
     * ventana de calificación.
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request evaluador
     * @return grupo actualizado
     */
    GroupResponse assignEvaluator(AuthenticatedUser actor, Long id, AssignEvaluatorRequest request);

    /**
     * Retira un evaluador que todavía no empezó a calificar el grupo, hasta que cierre
     * la ventana de calificación.
     *
     * @param actor       docente del grupo
     * @param id          grupo
     * @param evaluatorId evaluador
     */
    void removeEvaluator(AuthenticatedUser actor, Long id, Long evaluatorId);
}
