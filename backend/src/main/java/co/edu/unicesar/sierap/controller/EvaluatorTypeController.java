package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.service.EvaluatorTypeService;
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
 * Consulta y gestión del catálogo de tipos de evaluador externo (ADR-011, RF 11).
 */
@RestController
@RequestMapping("/api/evaluator-types")
public class EvaluatorTypeController {

    private final EvaluatorTypeService evaluatorTypeService;

    /**
     * Crea el controller.
     *
     * @param evaluatorTypeService servicio del catálogo
     */
    public EvaluatorTypeController(EvaluatorTypeService evaluatorTypeService) {
        this.evaluatorTypeService = evaluatorTypeService;
    }

    /**
     * Lista los tipos de evaluador en orden alfabético.
     *
     * @return tipos de evaluador
     */
    @GetMapping
    public List<EvaluatorTypeResponse> listAll() {
        return evaluatorTypeService.listAll();
    }

    /**
     * Crea un tipo de evaluador externo nuevo.
     *
     * @param request datos del tipo de evaluador
     * @return tipo creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EvaluatorTypeResponse create(@Valid @RequestBody NewEvaluatorTypeRequest request) {
        return evaluatorTypeService.create(request);
    }

    /**
     * Actualiza un tipo de evaluador externo existente.
     *
     * @param id      identificador del tipo
     * @param request nuevos datos
     * @return tipo actualizado
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EvaluatorTypeResponse update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateEvaluatorTypeRequest request) {
        return evaluatorTypeService.update(id, request);
    }

    /**
     * Elimina un tipo de evaluador externo sin evaluadores ni pesos asociados.
     *
     * @param id identificador del tipo
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        evaluatorTypeService.delete(id);
    }
}
