package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.GroupResultsResponse;
import co.edu.unicesar.sierap.dto.response.StudentResultResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ResultService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notas finales de los grupos y de los estudiantes ({@code docs/07} "Results").
 *
 * <p>Aquí solo se traduce HTTP: el alcance, las ventanas y el cálculo (ADR-018) están en
 * {@link ResultService}.</p>
 */
@RestController
@RequestMapping("/api")
public class ResultController {

    private final ResultService resultService;

    /**
     * Crea el controller.
     *
     * @param resultService notas finales
     */
    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * Nota del grupo y de cada integrante, en total y por RAP.
     *
     * @param actor el docente del grupo, el admin de su facultad o el super_admin
     * @param id    grupo
     * @return resultados del grupo; vista previa mientras la calificación está abierta
     */
    @GetMapping("/groups/{id}/results")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public GroupResultsResponse groupResults(@AuthenticationPrincipal AuthenticatedUser actor,
                                             @PathVariable Long id) {
        return resultService.groupResults(actor, id);
    }

    /**
     * Resultados del estudiante autenticado, uno por momento.
     *
     * @param actor estudiante
     * @return sus resultados, el más reciente primero
     */
    @GetMapping("/students/me/results")
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentResultResponse> myResults(@AuthenticationPrincipal AuthenticatedUser actor) {
        return resultService.myResults(actor);
    }
}
