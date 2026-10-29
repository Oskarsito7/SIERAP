package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.CopyMomentsRequest;
import co.edu.unicesar.sierap.dto.request.LaunchMomentRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.MomentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta y configuración de momentos (Etapa 3; contrato en {@code docs/07-contrato-api.md}).
 *
 * <p>Permite a los administradores crear, editar, eliminar y configurar los pesos
 * de los momentos mientras se encuentren en estado de configuración.</p>
 */
@RestController
@RequestMapping("/api/moments")
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public MomentDetailResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return momentService.get(actor, id);
    }

    /**
     * Crea un momento de evaluación en estado de configuración.
     *
     * @param actor   usuario autenticado (super_admin o admin de la facultad)
     * @param request datos del momento
     * @return detalle del momento creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MomentDetailResponse create(@AuthenticationPrincipal AuthenticatedUser actor,
                                       @Valid @RequestBody NewMomentRequest request) {
        return momentService.create(actor, request);
    }

    /**
     * Modifica parcialmente un momento en configuración.
     *
     * @param actor   usuario autenticado (super_admin o admin de la facultad)
     * @param id      momento
     * @param request campos a cambiar
     * @return detalle del momento actualizado
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MomentDetailResponse update(@AuthenticationPrincipal AuthenticatedUser actor,
                                       @PathVariable Long id,
                                       @Valid @RequestBody UpdateMomentRequest request) {
        return momentService.update(actor, id, request);
    }

    /**
     * Elimina un momento en configuración que no tenga grupos asociados.
     *
     * @param actor usuario autenticado (super_admin o admin de la facultad)
     * @param id    momento
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        momentService.delete(actor, id);
    }

    /**
     * Actualiza y reemplaza la distribución de pesos de calificación del momento.
     *
     * @param actor   usuario autenticado (super_admin o admin de la facultad)
     * @param id      momento
     * @param request nuevos pesos
     * @return pesos actualizados
     */
    @PutMapping("/{id}/weights")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MomentWeightsResponse updateWeights(@AuthenticationPrincipal AuthenticatedUser actor,
                                               @PathVariable Long id,
                                               @Valid @RequestBody MomentWeightsRequest request) {
        return momentService.updateWeights(actor, id, request);
    }

    /**
     * Diagnostica la configuración de un momento antes de lanzarlo.
     *
     * @param actor usuario autenticado (super_admin o admin de la facultad)
     * @param id    identificador del momento
     * @return diagnóstico de lanzamiento
     */
    @GetMapping("/{id}/launch-check")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public LaunchCheckResponse launchCheck(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @PathVariable Long id) {
        return momentService.launchCheck(actor, id);
    }

    /**
     * Lanza un momento a ejecución.
     *
     * @param actor   usuario autenticado (super_admin o admin de la facultad)
     * @param id      identificador del momento
     * @param request confirmación opcional de grupos por debajo del mínimo
     * @return detalle del momento lanzado
     */
    @PostMapping("/{id}/launch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MomentDetailResponse launch(@AuthenticationPrincipal AuthenticatedUser actor,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) LaunchMomentRequest request) {
        return momentService.launch(actor, id, request);
    }

    /**
     * Copia la configuración de momentos (RAP, criterios, niveles y pesos) de un
     * periodo a otro para los programas dentro del alcance.
     *
     * @param actor   usuario autenticado (super_admin o admin de facultad)
     * @param request parámetros de copia (periodos y programa opcional)
     * @return lista de nuevos momentos creados en el periodo destino
     */
    @PostMapping("/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<MomentSummaryResponse> copy(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @Valid @RequestBody CopyMomentsRequest request) {
        return momentService.copy(actor, request);
    }
}
