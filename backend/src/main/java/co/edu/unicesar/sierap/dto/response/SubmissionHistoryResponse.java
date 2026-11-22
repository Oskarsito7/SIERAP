package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Historial de entregas de un grupo ({@code docs/07}, "Submissions").
 *
 * @param versions            todas las versiones, de la más reciente a la más antigua
 * @param currentPresentation la versión más reciente que trae presentación;
 *                            {@code null} si ninguna la trae
 * @param maxVersions         máximo de versiones por grupo (parámetro global
 *                            {@code submission.max-versions}); {@code null} si el
 *                            parámetro falta o no es un entero válido
 */
public record SubmissionHistoryResponse(List<SubmissionResponse> versions, SubmissionResponse currentPresentation,
                                        Integer maxVersions) {
}
