package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Historial de entregas de un grupo ({@code docs/07}, "Submissions").
 *
 * @param versions            todas las versiones, de la más reciente a la más antigua
 * @param currentPresentation la versión más reciente que trae presentación;
 *                            {@code null} si ninguna la trae
 */
public record SubmissionHistoryResponse(List<SubmissionResponse> versions, SubmissionResponse currentPresentation) {
}
