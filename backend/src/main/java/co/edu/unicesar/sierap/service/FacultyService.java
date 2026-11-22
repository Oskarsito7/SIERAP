package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewFacultyRequest;
import co.edu.unicesar.sierap.dto.request.UpdateFacultyRequest;
import co.edu.unicesar.sierap.dto.response.FacultyResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Gestión del catálogo de facultades (RF 9, RF 11).
 */
public interface FacultyService {

    /**
     * Lista las facultades visibles para el usuario autenticado, ordenadas por nombre.
     *
     * @param actor usuario autenticado
     * @return facultades visibles
     */
    List<FacultyResponse> listForUser(AuthenticatedUser actor);

    /**
     * Crea una facultad nueva.
     *
     * @param request datos de la facultad
     * @return facultad creada
     */
    FacultyResponse create(NewFacultyRequest request);

    /**
     * Actualiza el código y nombre de una facultad.
     *
     * @param id      identificador de la facultad
     * @param request nuevos datos
     * @return facultad actualizada
     */
    FacultyResponse update(Long id, UpdateFacultyRequest request);

    /**
     * Elimina una facultad si no tiene grupos, estudiantes ni cuentas adscritas.
     *
     * @param id identificador de la facultad
     */
    void delete(Long id);
}
