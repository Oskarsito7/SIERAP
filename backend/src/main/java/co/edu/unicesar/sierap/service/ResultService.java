package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.GroupResultsResponse;
import co.edu.unicesar.sierap.dto.response.StudentResultResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Notas finales de los grupos y de los estudiantes ({@code docs/07} "Results",
 * ADR-018).
 */
public interface ResultService {

    /**
     * Nota del grupo y de cada integrante, en total y por RAP.
     *
     * @param actor   el docente del grupo, el admin de su facultad o el super_admin
     * @param groupId grupo
     * @return los resultados; mientras la calificación sigue abierta, una vista previa
     */
    GroupResultsResponse groupResults(AuthenticatedUser actor, Long groupId);

    /**
     * Resultados del estudiante, uno por momento en el que tiene grupo.
     *
     * @param actor estudiante
     * @return sus resultados, el más reciente primero
     */
    List<StudentResultResponse> myResults(AuthenticatedUser actor);
}
