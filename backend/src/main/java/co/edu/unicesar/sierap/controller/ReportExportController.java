package co.edu.unicesar.sierap.controller;

import java.nio.charset.StandardCharsets;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ReportExportService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Descarga de los reportes como archivos (Momento 2, ADR-020): sábana de notas en
 * Excel e informe, acta y boletín en PDF. Los roles son los mismos que los de
 * {@link ReportController}; el alcance fino (facultad, grupos propios, notas cerradas)
 * lo valida {@link ReportExportService}.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

    private final ReportExportService reportExportService;

    /**
     * Crea el controller.
     *
     * @param reportExportService exportación de reportes
     */
    public ReportExportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    /**
     * Sábana de notas del momento en Excel.
     *
     * @param actor    usuario autenticado
     * @param momentId momento
     * @return el archivo {@code .xlsx}
     */
    @GetMapping("/moments/{momentId}/xlsx")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Resource> momentWorkbook(@AuthenticationPrincipal AuthenticatedUser actor,
                                                   @PathVariable Long momentId) {
        return asAttachment(reportExportService.momentWorkbook(actor, momentId));
    }

    /**
     * Informe del momento en PDF.
     *
     * @param actor    usuario autenticado
     * @param momentId momento
     * @return el archivo PDF
     */
    @GetMapping("/moments/{momentId}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Resource> momentPdf(@AuthenticationPrincipal AuthenticatedUser actor,
                                              @PathVariable Long momentId) {
        return asAttachment(reportExportService.momentPdf(actor, momentId));
    }

    /**
     * Acta de evaluación del grupo en PDF.
     *
     * @param actor   usuario autenticado
     * @param groupId grupo
     * @return el archivo PDF
     */
    @GetMapping("/groups/{groupId}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Resource> groupPdf(@AuthenticationPrincipal AuthenticatedUser actor,
                                             @PathVariable Long groupId) {
        return asAttachment(reportExportService.groupPdf(actor, groupId));
    }

    /**
     * Boletín individual del estudiante en PDF.
     *
     * @param actor     usuario autenticado
     * @param momentId  momento
     * @param studentId estudiante (opcional para el propio estudiante)
     * @return el archivo PDF
     */
    @GetMapping("/individual/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> individualPdf(@AuthenticationPrincipal AuthenticatedUser actor,
                                                  @RequestParam Long momentId,
                                                  @RequestParam(required = false) Long studentId) {
        return asAttachment(reportExportService.individualPdf(actor, momentId, studentId));
    }

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
