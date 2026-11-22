package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.response.StudentImportPreviewResponse;
import co.edu.unicesar.sierap.dto.response.StudentImportResultResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.StudentImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga masiva de estudiantes por CSV ({@code docs/07} "Stage 2 — Accounts"): vista
 * previa sin escribir y creación de las filas válidas.
 *
 * <p>Aquí se valida el <b>rol</b>; el alcance (facultad del programa) lo valida
 * {@link StudentImportService}.</p>
 */
@RestController
@RequestMapping("/api/students/bulk")
public class StudentImportController {

    private final StudentImportService studentImportService;

    /**
     * Crea el controller.
     *
     * @param studentImportService servicio de carga masiva
     */
    public StudentImportController(StudentImportService studentImportService) {
        this.studentImportService = studentImportService;
    }

    /**
     * Valida el archivo y muestra qué se crearía, sin escribir nada.
     *
     * @param actor     usuario autenticado
     * @param programId programa destino
     * @param file      archivo CSV
     * @return filas con sus errores y los totales
     */
    @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public StudentImportPreviewResponse preview(@AuthenticationPrincipal AuthenticatedUser actor,
                                                @RequestParam Long programId,
                                                @RequestPart(name = "file", required = false) MultipartFile file) {
        return studentImportService.preview(actor, programId, file);
    }

    /**
     * Crea los estudiantes de las filas válidas del archivo.
     *
     * @param actor     usuario autenticado
     * @param programId programa destino
     * @param file      archivo CSV
     * @return estudiantes creados y filas con error
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public StudentImportResultResponse importStudents(@AuthenticationPrincipal AuthenticatedUser actor,
                                                      @RequestParam Long programId,
                                                      @RequestPart(name = "file", required = false)
                                                      MultipartFile file) {
        return studentImportService.importStudents(actor, programId, file);
    }
}
