package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.response.RegistrationCodeResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.RegistrationCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * @param page  página pedida, desde 0 (opcional, ADR-024)
     * @param size  tamaño de página de 1 a 200 (opcional)
     * @return códigos de registro; con {@code page} o {@code size} es la página pedida y
     *         {@code X-Total-Count} trae el total
     */
    @GetMapping
    public ResponseEntity<List<RegistrationCodeResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(PagedResponses.MIN_SIZE) @Max(PagedResponses.MAX_SIZE) Integer size) {
        return PagedResponses.respond(registrationCodeService.list(actor, PagedResponses.pageable(page, size)),
                PagedResponses.isPaged(page, size));
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
