package co.edu.unicesar.sierap.mapper;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.SubmissionHistoryResponse;
import co.edu.unicesar.sierap.dto.response.SubmissionResponse;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Submission;

/**
 * Convierte las entregas a sus DTO de respuesta ({@code docs/07}, "Submissions").
 */
public final class SubmissionMapper {

    private SubmissionMapper() {
    }

    /**
     * Historial de entregas de un grupo.
     *
     * @param group grupo con sus versiones cargadas, de la más antigua a la más reciente
     * @return versiones de la más reciente a la más antigua y la presentación vigente
     */
    public static SubmissionHistoryResponse toHistory(ProjectGroup group) {
        Submission toGrade = group.getSubmissionToGrade();
        List<SubmissionResponse> versions = group.getSubmissions().reversed().stream()
                .map(submission -> toResponse(submission, toGrade))
                .toList();
        SubmissionResponse currentPresentation = versions.stream()
                .filter(version -> version.presentationFileName() != null)
                .findFirst()
                .orElse(null);
        return new SubmissionHistoryResponse(versions, currentPresentation);
    }

    /**
     * Una versión de la entrega.
     *
     * @param submission versión
     * @param toGrade    versión que se califica en su grupo, o {@code null} si no hay
     * @return la versión, marcando si es la que se califica
     */
    public static SubmissionResponse toResponse(Submission submission, Submission toGrade) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProjectGroup().getId(),
                submission.getVersionNumber(),
                submission.getSubmittedAt(),
                submission.isLate(),
                submission.getZipFileName(),
                submission.getZipSize(),
                submission.getPresentationFileName(),
                submission.getPresentationSize(),
                toGrade != null && toGrade.getVersionNumber() == submission.getVersionNumber());
    }
}
