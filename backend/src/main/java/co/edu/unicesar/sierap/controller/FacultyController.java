package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewFacultyRequest;
import co.edu.unicesar.sierap.dto.request.UpdateFacultyRequest;
import co.edu.unicesar.sierap.dto.response.FacultyResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.FacultyService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del catálogo de facultades (RF 9, RF 11).
 */
@RestController
@RequestMapping("/api/faculties")
public class FacultyController {

    private final FacultyService facultyService;

    /**
     * Crea el controller.
     *
     * @param facultyService servicio de facultades
     */
    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    /**
     * Lista las facultades visibles según el rol del usuario autenticado.
     *
     * @param actor usuario autenticado
     * @return facultades ordenadas por nombre
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public List<FacultyResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return facultyService.listForUser(actor);
    }

    /**
     * Crea una facultad nueva.
     *
     * @param request datos de la facultad
     * @return facultad creada
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public FacultyResponse create(@Valid @RequestBody NewFacultyRequest request) {
        return facultyService.create(request);
    }

    /**
     * Actualiza una facultad existente.
     *
     * @param id      identificador de la facultad
     * @param request nuevos datos
     * @return facultad actualizada
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public FacultyResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateFacultyRequest request) {
        return facultyService.update(id, request);
    }

    /**
     * Elimina una facultad sin dependencias.
     *
     * @param id identificador de la facultad
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        facultyService.delete(id);
    }
}
