package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.request.UpdateAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.response.AcademicPeriodResponse;
import co.edu.unicesar.sierap.service.AcademicPeriodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Endpoints del catálogo de periodos académicos (RF 9, RF 11, ADR-010).
 */
@RestController
@RequestMapping("/api/periods")
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    /**
     * Crea el controller.
     *
     * @param academicPeriodService servicio de periodos académicos
     */
    public AcademicPeriodController(AcademicPeriodService academicPeriodService) {
        this.academicPeriodService = academicPeriodService;
    }

    /**
     * Lista todos los periodos académicos del más reciente al más antiguo.
     *
     * @return periodos académicos
     */
    @GetMapping
    public List<AcademicPeriodResponse> listAll() {
        return academicPeriodService.listAll();
    }

    /**
     * Crea un periodo académico nuevo.
     *
     * @param request datos del periodo
     * @return periodo creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AcademicPeriodResponse create(@Valid @RequestBody NewAcademicPeriodRequest request) {
        return academicPeriodService.create(request);
    }

    /**
     * Actualiza un periodo académico existente.
     *
     * @param id      identificador del periodo
     * @param request nuevos datos
     * @return periodo actualizado
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AcademicPeriodResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateAcademicPeriodRequest request) {
        return academicPeriodService.update(id, request);
    }

    /**
     * Elimina un periodo académico sin momentos configurados.
     *
     * @param id identificador del periodo
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        academicPeriodService.delete(id);
    }
}
