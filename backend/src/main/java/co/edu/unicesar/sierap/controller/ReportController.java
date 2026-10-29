package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.response.report.ComparativeReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del módulo de reportes académicos (Etapa 7, RF 12).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    /**
     * Construye el controlador de reportes.
     *
     * @param reportService servicio de reportes
     */
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Reporte individual de un estudiante en un momento.
     *
     * @param actor     usuario autenticado
     * @param momentId  momento evaluativo
     * @param studentId estudiante consultado (opcional para estudiantes)
     * @return datos estructurados del reporte individual
     */
    @GetMapping("/individual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public IndividualReportData getIndividualReport(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam Long momentId,
            @RequestParam(required = false) Long studentId) {
        return reportService.getIndividualReport(actor, momentId, studentId);
    }

    /**
     * Reporte por grupo de proyecto de aula.
     *
     * @param actor   usuario autenticado
     * @param groupId identificador del grupo
     * @return datos estructurados del reporte por grupo
     */
    @GetMapping("/groups/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public GroupReportData getGroupReport(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long groupId) {
        return reportService.getGroupReport(actor, groupId);
    }

    /**
     * Reporte consolidado de un momento y programa académico.
     *
     * @param actor    usuario autenticado
     * @param momentId identificador del momento
     * @return datos estructurados del reporte consolidado
     */
    @GetMapping("/moments/{momentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public MomentReportData getMomentReport(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long momentId) {
        return reportService.getMomentReport(actor, momentId);
    }

    /**
     * Reporte comparativo entre dos semestres o periodos para el mismo programa y momento.
     *
     * @param actor        usuario autenticado
     * @param programId    programa académico
     * @param momentNumber número de momento (p. ej. 1)
     * @param periodAId    periodo base A
     * @param periodBId    periodo objetivo B
     * @return datos estructurados del reporte comparativo
     */
    @GetMapping("/comparative")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ComparativeReportData getComparativeReport(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam Long programId,
            @RequestParam int momentNumber,
            @RequestParam Long periodAId,
            @RequestParam Long periodBId) {
        return reportService.getComparativeReport(actor, programId, momentNumber, periodAId, periodBId);
    }
}
