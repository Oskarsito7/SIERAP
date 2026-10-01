package co.edu.unicesar.sierap.controller;

import java.nio.charset.StandardCharsets;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.dto.response.SubmissionHistoryResponse;
import co.edu.unicesar.sierap.dto.response.SubmissionResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.SubmissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entregas de los grupos (Etapa 5, {@code docs/07} "Submissions"): subir una versión,
 * ver el historial y descargar sus archivos.
 *
 * <p>Aquí solo se traduce HTTP: el alcance, la ventana de entregas y la validación de
 * los archivos los hace {@link SubmissionService}.</p>
 */
@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * Crea el controller.
     *
     * @param submissionService service de entregas
     */
    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * Sube una versión nueva de la entrega del grupo.
     *
     * @param actor        integrante del grupo
     * @param id           grupo
     * @param zip          proyecto en {@code .zip}
     * @param presentation presentación opcional
     * @return la versión creada
     */
    @PostMapping(path = "/groups/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionResponse upload(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                     @RequestPart(name = "zip", required = false) MultipartFile zip,
                                     @RequestPart(name = "presentation", required = false)
                                     MultipartFile presentation) {
        return submissionService.upload(actor, id, zip, presentation);
    }

    /**
     * Historial de entregas del grupo.
     *
     * @param actor usuario autenticado
     * @param id    grupo
     * @return versiones de la más reciente a la más antigua y la presentación vigente
     */
    @GetMapping("/groups/{id}/submissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR', 'STUDENT')")
    public SubmissionHistoryResponse history(@AuthenticationPrincipal AuthenticatedUser actor,
                                             @PathVariable Long id) {
        return submissionService.history(actor, id);
    }

    /**
     * Descarga el {@code .zip} de una versión.
     *
     * @param actor usuario autenticado
     * @param id    versión
     * @return el archivo como adjunto
     */
    @GetMapping("/submissions/{id}/zip")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR', 'STUDENT')")
    public ResponseEntity<Resource> downloadZip(@AuthenticationPrincipal AuthenticatedUser actor,
                                                @PathVariable Long id) {
        return asAttachment(submissionService.downloadZip(actor, id));
    }

    /**
     * Descarga la presentación de una versión.
     *
     * @param actor usuario autenticado
     * @param id    versión
     * @return el archivo como adjunto; {@code 404} si esa versión no la trae
     */
    @GetMapping("/submissions/{id}/presentation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'EXTERNAL_EVALUATOR', 'STUDENT')")
    public ResponseEntity<Resource> downloadPresentation(@AuthenticationPrincipal AuthenticatedUser actor,
                                                         @PathVariable Long id) {
        return asAttachment(submissionService.downloadPresentation(actor, id));
    }

    /** Respuesta con el archivo y su nombre original (codificado en UTF-8 si hace falta). */
    private static ResponseEntity<Resource> asAttachment(FileDownload file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .body(file.resource());
    }
}
