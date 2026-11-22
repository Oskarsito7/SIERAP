package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;

/**
 * Consulta y gestión del catálogo de tipos de evaluador externo (ADR-011, RF 11).
 */
public interface EvaluatorTypeService {

    /**
     * Lista todos los tipos del catálogo en orden alfabético.
     *
     * @return tipos de evaluador
     */
    List<EvaluatorTypeResponse> listAll();

    /**
     * Crea un tipo de evaluador externo nuevo.
     *
     * @param request datos del tipo de evaluador
     * @return tipo de evaluador creado
     */
    EvaluatorTypeResponse create(NewEvaluatorTypeRequest request);

    /**
     * Actualiza el nombre, descripción y estado activo de un tipo de evaluador.
     *
     * @param id      identificador del tipo de evaluador
     * @param request nuevos datos
     * @return tipo de evaluador actualizado
     */
    EvaluatorTypeResponse update(Long id, UpdateEvaluatorTypeRequest request);

    /**
     * Elimina un tipo de evaluador si ningún evaluador ni peso de momento lo utiliza.
     *
     * @param id identificador del tipo de evaluador
     */
    void delete(Long id);
}
