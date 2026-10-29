package co.edu.unicesar.sierap.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambios a la evaluación propia de un grupo ({@code PUT /api/groups/{id}/evaluation},
 * {@code docs/07}). Todos los campos son opcionales: lo que no se envía se conserva.
 *
 * @param levels      nivel del grupo en cada criterio enviado; los demás conservan el suyo
 * @param comment     observación general para el grupo; {@code null} la conserva y un
 *                    texto vacío la borra
 * @param adjustments ajustes individuales; cuando se envía, <b>reemplaza</b> todos los
 *                    anteriores, y {@code null} los conserva (ADR-018)
 */
public record SaveEvaluationRequest(
        @Valid List<LevelChoice> levels,
        @Size(max = 2000, message = "La observación admite hasta 2000 caracteres") String comment,
        @Valid List<AdjustmentChoice> adjustments) {

    /**
     * Nivel del grupo en un criterio.
     *
     * @param criterionId criterio del momento
     * @param levelId     nivel de ese criterio
     */
    public record LevelChoice(
            @NotNull(message = "El criterio es obligatorio") Long criterionId,
            @NotNull(message = "El nivel es obligatorio") Long levelId) {
    }

    /**
     * Nivel de un criterio para un solo integrante.
     *
     * @param studentId   integrante del grupo
     * @param criterionId criterio del momento
     * @param levelId     nivel de ese criterio
     */
    public record AdjustmentChoice(
            @NotNull(message = "El estudiante es obligatorio") Long studentId,
            @NotNull(message = "El criterio es obligatorio") Long criterionId,
            @NotNull(message = "El nivel es obligatorio") Long levelId) {
    }
}
