package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewProgramRequest;
import co.edu.unicesar.sierap.dto.request.UpdateProgramRequest;
import co.edu.unicesar.sierap.dto.response.ProgramResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Gestión del catálogo de programas académicos (RF 9, RF 11).
 */
public interface ProgramService {

    /**
     * Lista los programas visibles para el usuario autenticado, ordenados por nombre.
     *
     * @param actor     usuario autenticado
     * @param facultyId filtro opcional de facultad
     * @return programas visibles
     */
    List<ProgramResponse> listForUser(AuthenticatedUser actor, Long facultyId);

    /**
     * Crea un programa académico nuevo en una facultad.
     *
     * @param request datos del programa
     * @return programa creado
     */
    ProgramResponse create(NewProgramRequest request);

    /**
     * Actualiza el código y nombre de un programa académico.
     *
     * @param id      identificador del programa
     * @param request nuevos datos
     * @return programa actualizado
     */
    ProgramResponse update(Long id, UpdateProgramRequest request);

    /**
     * Elimina un programa si no tiene grupos, estudiantes ni evaluadores externos asociados.
     *
     * @param id identificador del programa
     */
    void delete(Long id);
}
