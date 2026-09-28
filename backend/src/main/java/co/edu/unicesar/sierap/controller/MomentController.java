package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.MomentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de momentos (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 *
 * <p>Por ahora solo lectura: las pantallas de grupos, entregas y evaluación los
 * necesitan. La configuración (crear, editar, lanzar) se agrega en este mismo
 * recurso más adelante en la Etapa 3.</p>
 */
@RestController
@RequestMapping("/api/moments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
public class MomentController {

    private final MomentService momentService;

    /**
     * Crea el controller.
     *
     * @param momentService servicio de momentos
     */
    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    /**
     * Lista los momentos de un periodo dentro del alcance del usuario.
     *
     * @param actor    usuario autenticado
     * @param periodId periodo académico; si se omite, el periodo en curso
     * @return momentos
     */
    @GetMapping
    public List<MomentSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @RequestParam(required = false) Long periodId) {
        return momentService.list(actor, periodId);
    }

    /**
     * Consulta un momento con sus RAP, criterios y niveles.
     *
     * @param actor usuario autenticado
     * @param id    momento
     * @return detalle del momento
     */
    @GetMapping("/{id}")
    public MomentDetailResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return momentService.get(actor, id);
    }
}
