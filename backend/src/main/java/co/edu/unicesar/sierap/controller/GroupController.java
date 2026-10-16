package co.edu.unicesar.sierap.controller;

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
import co.edu.unicesar.sierap.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grupos de un momento y sus evaluadores externos ({@code docs/07} "Groups").
 *
 * <p>Incluye las rutas de los estudiantes (Etapa 5): inscribirse, salirse y su
 * historial. Aquí solo se traduce HTTP: el alcance y las ventanas los valida
 * {@link GroupService}.</p>
 */
@RestController
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;

    /**
     * Crea el controller.
     *
     * @param groupService service de grupos
     */
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * Lista los grupos del alcance del usuario.
     *
     * @param actor    usuario autenticado
     * @param momentId momento (obligatorio para estudiantes, admin y super_admin)
     * @return grupos ordenados por nombre
     */
    @GetMapping("/groups")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR', 'STUDENT')")
    public List<GroupResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                    @RequestParam(required = false) Long momentId) {
        return groupService.list(actor, momentId);
    }

    /**
     * Consulta un grupo.
     *
     * @param actor usuario autenticado
     * @param id    grupo
     * @return el grupo
     */
    @GetMapping("/groups/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR', 'STUDENT')")
    public GroupResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return groupService.get(actor, id);
    }

    /**
     * Habilita un grupo.
     *
     * @param actor   docente
     * @param request momento y nombre
     * @return grupo creado
     */
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public GroupResponse create(@AuthenticationPrincipal AuthenticatedUser actor,
                                @Valid @RequestBody NewGroupRequest request) {
        return groupService.create(actor, request);
    }

    /**
     * Inscribe al estudiante en el grupo; si estaba en otro del mismo momento, sale de
     * él en el mismo paso.
     *
     * @param actor estudiante
     * @param id    grupo
     * @return grupo actualizado
     */
    @PostMapping("/groups/{id}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public GroupResponse join(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return groupService.join(actor, id);
    }

    /**
     * Retira al estudiante del grupo.
     *
     * @param actor estudiante integrante
     * @param id    grupo
     */
    @PostMapping("/groups/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void leave(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        groupService.leave(actor, id);
    }

    /**
     * Historial de grupos del estudiante.
     *
     * @param actor estudiante
     * @return sus grupos, uno por momento, el más reciente primero
     */
    @GetMapping("/students/me/groups")
    @PreAuthorize("hasRole('STUDENT')")
    public List<GroupResponse> myGroups(@AuthenticationPrincipal AuthenticatedUser actor) {
        return groupService.myGroups(actor);
    }

    /**
     * Cambia el nombre o las fechas de un grupo.
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request campos a cambiar
     * @return grupo actualizado
     */
    @PatchMapping("/groups/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupResponse update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.update(actor, id, request);
    }

    /**
     * Elimina un grupo sin integrantes antes del lanzamiento.
     *
     * @param actor docente del grupo
     * @param id    grupo
     */
    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        groupService.delete(actor, id);
    }

    /**
     * Cambia el título del proyecto.
     *
     * @param actor   docente o integrante del grupo
     * @param id      grupo
     * @param request título
     * @return grupo actualizado
     */
    @PutMapping("/groups/{id}/project-title")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public GroupResponse updateProjectTitle(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                            @Valid @RequestBody ProjectTitleRequest request) {
        return groupService.updateProjectTitle(actor, id, request);
    }

    /**
     * Estudiantes del programa del momento sin grupo en él.
     *
     * @param actor docente o admin de la facultad
     * @param id    momento
     * @return estudiantes sin grupo
     */
    @GetMapping("/moments/{id}/students-without-group")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<StudentWithoutGroupResponse> studentsWithoutGroup(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                  @PathVariable Long id) {
        return groupService.studentsWithoutGroup(actor, id);
    }

    /**
     * Agrega un estudiante sin grupo a uno de los grupos del docente.
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request estudiante
     * @return grupo actualizado
     */
    @PostMapping("/groups/{id}/members")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupResponse addMember(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                   @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(actor, id, request);
    }

    /**
     * Retira a un integrante del grupo.
     *
     * @param actor     docente del grupo
     * @param id        grupo
     * @param studentId integrante
     */
    @DeleteMapping("/groups/{id}/members/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void removeMember(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                             @PathVariable Long studentId) {
        groupService.removeMember(actor, id, studentId);
    }

    /**
     * Busca un evaluador externo activo por correo o documento exactos.
     *
     * @param q correo o número de documento
     * @return el evaluador
     */
    @GetMapping("/external-evaluators/lookup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public UserSummaryResponse lookupExternalEvaluator(@RequestParam String q) {
        return groupService.lookupExternalEvaluator(q);
    }

    /**
     * Asigna un evaluador externo a un grupo.
     *
     * @param actor   docente del grupo
     * @param id      grupo
     * @param request evaluador
     * @return grupo actualizado
     */
    @PostMapping("/groups/{id}/evaluators")
    @PreAuthorize("hasRole('TEACHER')")
    public GroupResponse assignEvaluator(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                         @Valid @RequestBody AssignEvaluatorRequest request) {
        return groupService.assignEvaluator(actor, id, request);
    }

    /**
     * Retira un evaluador externo de un grupo.
     *
     * @param actor       docente del grupo
     * @param id          grupo
     * @param evaluatorId evaluador
     */
    @DeleteMapping("/groups/{id}/evaluators/{evaluatorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void removeEvaluator(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                @PathVariable Long evaluatorId) {
        groupService.removeEvaluator(actor, id, evaluatorId);
    }
}
