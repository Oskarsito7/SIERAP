package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAdminRequest;
import co.edu.unicesar.sierap.dto.request.NewExternalEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.request.NewTeacherRequest;
import co.edu.unicesar.sierap.dto.request.UpdateUserRequest;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creación y gestión de cuentas (Etapa 2, ADR-017; contrato en
 * {@code docs/07-contrato-api.md}).
 *
 * <p>Aquí se valida el <b>rol</b> con {@code @PreAuthorize}; el <b>alcance</b> (que
 * la facultad o la cuenta sean del usuario) lo valida {@link AccountService}.</p>
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    /**
     * Crea el controller.
     *
     * @param accountService servicio de cuentas
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Crea el administrador de una facultad.
     *
     * @param actor   usuario autenticado
     * @param request datos del administrador
     * @return usuario creado
     */
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserSummaryResponse createAdmin(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @Valid @RequestBody NewAdminRequest request) {
        return accountService.createAdmin(actor, request);
    }

    /**
     * Crea un docente.
     *
     * @param actor   usuario autenticado
     * @param request datos del docente
     * @return usuario creado
     */
    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public UserSummaryResponse createTeacher(@AuthenticationPrincipal AuthenticatedUser actor,
                                             @Valid @RequestBody NewTeacherRequest request) {
        return accountService.createTeacher(actor, request);
    }

    /**
     * Lista los docentes de una facultad.
     *
     * @param actor     usuario autenticado
     * @param facultyId facultad
     * @return docentes
     */
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<UserSummaryResponse> listTeachers(@AuthenticationPrincipal AuthenticatedUser actor,
                                                  @RequestParam Long facultyId) {
        return accountService.listTeachers(actor, facultyId);
    }

    /**
     * Crea un estudiante en un programa.
     *
     * @param actor   usuario autenticado
     * @param request datos del estudiante
     * @return usuario creado
     */
    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public UserSummaryResponse createStudent(@AuthenticationPrincipal AuthenticatedUser actor,
                                             @Valid @RequestBody NewStudentRequest request) {
        return accountService.createStudent(actor, request);
    }

    /**
     * Lista los estudiantes de un programa.
     *
     * @param actor     usuario autenticado
     * @param programId programa
     * @return estudiantes
     */
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public List<UserSummaryResponse> listStudents(@AuthenticationPrincipal AuthenticatedUser actor,
                                                  @RequestParam Long programId) {
        return accountService.listStudents(actor, programId);
    }

    /**
     * Crea un evaluador externo.
     *
     * @param actor   usuario autenticado
     * @param request datos del evaluador
     * @return usuario creado
     */
    @PostMapping("/external-evaluators")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public UserSummaryResponse createExternalEvaluator(@AuthenticationPrincipal AuthenticatedUser actor,
                                                       @Valid @RequestBody NewExternalEvaluatorRequest request) {
        return accountService.createExternalEvaluator(actor, request);
    }

    /**
     * Lista los evaluadores externos dentro del alcance del usuario.
     *
     * @param actor usuario autenticado
     * @return evaluadores externos
     */
    @GetMapping("/external-evaluators")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public List<UserSummaryResponse> listExternalEvaluators(@AuthenticationPrincipal AuthenticatedUser actor) {
        return accountService.listExternalEvaluators(actor);
    }

    /**
     * Reenvía a una cuenta el enlace para crear o recuperar su contraseña.
     *
     * @param actor usuario autenticado
     * @param id    cuenta destino
     */
    @PostMapping("/users/{id}/password-link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public void resendPasswordLink(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        accountService.resendPasswordLink(actor, id);
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * @param actor   usuario autenticado
     * @param id      cuenta destino
     * @param request nuevo estado
     * @return usuario actualizado
     */
    @PatchMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public UserSummaryResponse updateUser(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return accountService.updateUser(actor, id, request);
    }
}
