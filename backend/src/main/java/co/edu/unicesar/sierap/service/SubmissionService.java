package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.dto.response.SubmissionHistoryResponse;
import co.edu.unicesar.sierap.dto.response.SubmissionResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entregas de los grupos (Etapa 5, {@code docs/07} "Submissions"): los integrantes
 * suben versiones del proyecto y la presentación durante la ventana de entregas, y el
 * grupo, su docente, sus evaluadores y los administradores las consultan y descargan.
 */
public interface SubmissionService {

    /**
     * Sube una versión nueva de la entrega; nunca reemplaza las anteriores. Queda
     * tardía si hoy es después de la fecha límite del grupo (o del fin de la ventana,
     * si el docente no fijó una).
     *
     * @param actor        integrante del grupo
     * @param groupId      grupo
     * @param zip          proyecto en {@code .zip}, obligatorio
     * @param presentation presentación {@code .pdf}, {@code .docx} o {@code .pptx}; opcional
     * @return la versión creada
     */
    SubmissionResponse upload(AuthenticatedUser actor, Long groupId, MultipartFile zip, MultipartFile presentation);

    /**
     * Historial de entregas de un grupo.
     *
     * @param actor   integrante, docente del grupo, evaluador asignado, admin de la
     *                facultad o super_admin
     * @param groupId grupo
     * @return versiones de la más reciente a la más antigua y la presentación vigente
     */
    SubmissionHistoryResponse history(AuthenticatedUser actor, Long groupId);

    /**
     * Descarga el {@code .zip} de una versión.
     *
     * @param actor        mismo alcance que {@link #history(AuthenticatedUser, Long)}
     * @param submissionId versión
     * @return el archivo con su nombre original
     */
    FileDownload downloadZip(AuthenticatedUser actor, Long submissionId);

    /**
     * Descarga la presentación de una versión.
     *
     * @param actor        mismo alcance que {@link #history(AuthenticatedUser, Long)}
     * @param submissionId versión
     * @return el archivo con su nombre original; {@code 404} si esa versión no la trae
     */
    FileDownload downloadPresentation(AuthenticatedUser actor, Long submissionId);
}
