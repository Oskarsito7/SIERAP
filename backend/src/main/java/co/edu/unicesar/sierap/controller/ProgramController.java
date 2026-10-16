package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewProgramRequest;
import co.edu.unicesar.sierap.dto.request.UpdateProgramRequest;
import co.edu.unicesar.sierap.dto.response.ProgramResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints del catálogo de programas académicos (RF 9, RF 11).
 */
@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    /**
     * Crea el controller.
     *
     * @param programService servicio de programas
     */
    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    /**
     * Lista los programas visibles según el rol del usuario autenticado.
     *
     * @param actor     usuario autenticado
     * @param facultyId filtro opcional de facultad
     * @return programas ordenados por nombre
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public List<ProgramResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                      @RequestParam(required = false) Long facultyId) {
        return programService.listForUser(actor, facultyId);
    }

    /**
     * Crea un programa académico nuevo.
     *
     * @param request datos del programa
     * @return programa creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProgramResponse create(@Valid @RequestBody NewProgramRequest request) {
        return programService.create(request);
    }

    /**
     * Actualiza un programa académico existente.
     *
     * @param id      identificador del programa
     * @param request nuevos datos
     * @return programa actualizado
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProgramResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateProgramRequest request) {
        return programService.update(id, request);
    }

    /**
     * Elimina un programa académico sin dependencias.
     *
     * @param id identificador del programa
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        programService.delete(id);
    }
}
