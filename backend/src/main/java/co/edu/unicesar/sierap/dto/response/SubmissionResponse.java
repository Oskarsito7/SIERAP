package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;

/**
 * Versión de la entrega de un grupo, {@code Submission} en {@code docs/07}.
 *
 * @param id                   identificador
 * @param groupId              grupo dueño de la entrega
 * @param versionNumber        número de versión, consecutivo por grupo
 * @param submittedAt          fecha y hora de la carga
 * @param late                 {@code true} si llegó después de la fecha límite del grupo
 * @param zipFileName          nombre original del {@code .zip}, solo para mostrarlo
 * @param zipSize              tamaño del {@code .zip} en bytes
 * @param presentationFileName nombre original de la presentación; {@code null} si esta
 *                             versión no la trae
 * @param presentationSize     tamaño de la presentación en bytes; {@code null} si no la trae
 * @param toGrade              {@code true} si es la versión que califican los evaluadores
 */
public record SubmissionResponse(
        Long id,
        Long groupId,
        int versionNumber,
        LocalDateTime submittedAt,
        boolean late,
        String zipFileName,
        long zipSize,
        String presentationFileName,
        Long presentationSize,
        boolean toGrade) {
}
