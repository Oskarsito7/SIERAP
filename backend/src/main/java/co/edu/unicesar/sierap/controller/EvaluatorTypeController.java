package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.service.EvaluatorTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del catálogo de tipos de evaluador externo (ADR-011), para los
 * formularios de creación de evaluadores. Basta con estar autenticado.
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
}
