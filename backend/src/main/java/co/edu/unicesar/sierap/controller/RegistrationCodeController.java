package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.response.RegistrationCodeResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.RegistrationCodeService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Códigos con los que los estudiantes se registran solos (ADR-017; contrato en
 * {@code docs/07-contrato-api.md}).
 *
 * <p>Los genera el docente, o un admin o el super_admin a nombre de un docente de
 * su alcance.</p>
 */
@RestController
@RequestMapping("/api/registration-codes")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
public class RegistrationCodeController {

    private final RegistrationCodeService registrationCodeService;

    /**
     * Crea el controller.
     *
     * @param registrationCodeService servicio de códigos de registro
     */
    public RegistrationCodeController(RegistrationCodeService registrationCodeService) {
        this.registrationCodeService = registrationCodeService;
    }

    /**
     * Genera un código de registro.
     *
     * @param actor   usuario autenticado
     * @param request programa, vencimiento y, si no lo pide el docente, el docente dueño
     * @return código generado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationCodeResponse create(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @Valid @RequestBody NewRegistrationCodeRequest request) {
        return registrationCodeService.create(actor, request);
    }

    /**
     * Lista los códigos dentro del alcance del usuario.
     *
     * @param actor usuario autenticado
     * @return códigos de registro
     */
    @GetMapping
    public List<RegistrationCodeResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return registrationCodeService.list(actor);
    }

    /**
     * Desactiva un código de registro.
     *
     * @param actor   usuario autenticado
     * @param id      código
     * @param request nuevo estado
     * @return código actualizado
     */
    @PatchMapping("/{id}")
    public RegistrationCodeResponse update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                           @Valid @RequestBody UpdateRegistrationCodeRequest request) {
        return registrationCodeService.update(actor, id, request);
    }
}
