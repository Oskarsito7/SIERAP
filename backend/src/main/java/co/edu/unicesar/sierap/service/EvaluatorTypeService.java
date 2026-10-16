package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;

/**
 * Consulta del catálogo de tipos de evaluador externo (ADR-011).
 */
public interface EvaluatorTypeService {

    /**
     * Lista todos los tipos del catálogo en orden alfabético.
     *
     * @return tipos de evaluador
     */
    List<EvaluatorTypeResponse> listAll();
}
