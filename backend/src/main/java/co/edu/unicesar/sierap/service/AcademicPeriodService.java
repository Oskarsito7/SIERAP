package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.request.UpdateAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.response.AcademicPeriodResponse;

/**
 * Gestión del catálogo de periodos académicos (RF 9, RF 11, ADR-010).
 */
public interface AcademicPeriodService {

    /**
     * Lista todos los periodos académicos del más reciente al más antiguo.
     *
     * @return periodos académicos
     */
    List<AcademicPeriodResponse> listAll();

    /**
     * Crea un periodo académico nuevo.
     *
     * @param request datos del periodo
     * @return periodo creado
     */
    AcademicPeriodResponse create(NewAcademicPeriodRequest request);

    /**
     * Actualiza un periodo académico existente.
     *
     * @param id      identificador del periodo
     * @param request nuevos datos del periodo
     * @return periodo actualizado
     */
    AcademicPeriodResponse update(Long id, UpdateAcademicPeriodRequest request);

    /**
     * Elimina un periodo académico si no tiene momentos configurados.
     *
     * @param id identificador del periodo
     */
    void delete(Long id);
}
