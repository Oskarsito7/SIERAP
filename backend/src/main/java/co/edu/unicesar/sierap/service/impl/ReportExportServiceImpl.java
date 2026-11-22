package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.entity.GroupReport;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ReportExportService;
import co.edu.unicesar.sierap.service.ReportService;
import co.edu.unicesar.sierap.service.export.ExportFormat;
import co.edu.unicesar.sierap.service.export.MomentWorkbookWriter;
import co.edu.unicesar.sierap.service.export.ReportPdfWriter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ReportExportService}.
 *
 * <p>Primero pide el reporte a {@link ReportService}, que valida quién puede verlo
 * (403/404/422 con el mismo mensaje que en pantalla), y después lo entrega a
 * {@link MomentWorkbookWriter} o {@link ReportPdfWriter}, que solo dan formato. Así un
 * archivo nunca muestra algo que el usuario no vería en la pantalla de reportes.</p>
 */
@Service
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF = "application/pdf";

    private final ReportService reportService;
    private final ProjectGroupRepository projectGroupRepository;
    private final AccessScope accessScope;
    private final GradeScaleLoader gradeScaleLoader;
    private final MomentWorkbookWriter workbookWriter;
    private final ReportPdfWriter pdfWriter;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param reportService          reportes con sus reglas de alcance
     * @param projectGroupRepository grupos del momento
     * @param accessScope            carga del usuario autenticado
     * @param gradeScaleLoader       umbral de aprobación
     * @param workbookWriter         generador del Excel
     * @param pdfWriter              generador de los PDF
     * @param clock                  reloj de la aplicación
     */
    public ReportExportServiceImpl(ReportService reportService, ProjectGroupRepository projectGroupRepository,
                                   AccessScope accessScope, GradeScaleLoader gradeScaleLoader,
                                   MomentWorkbookWriter workbookWriter, ReportPdfWriter pdfWriter, Clock clock) {
        this.reportService = reportService;
        this.projectGroupRepository = projectGroupRepository;
        this.accessScope = accessScope;
        this.gradeScaleLoader = gradeScaleLoader;
        this.workbookWriter = workbookWriter;
        this.pdfWriter = pdfWriter;
        this.clock = clock;
    }

    @Override
    public FileDownload momentWorkbook(AuthenticatedUser actor, Long momentId) {
        MomentReportData summary = reportService.getMomentReport(actor, momentId);
        User viewer = accessScope.loadActor(actor);
        byte[] content = workbookWriter.write(summary, visibleGroups(viewer, momentId), gradeScaleLoader.load(),
                viewer.getFullName());
        return file(content, "sabana-notas-" + momentName(summary) + ".xlsx", XLSX);
    }

    @Override
    public FileDownload momentPdf(AuthenticatedUser actor, Long momentId) {
        MomentReportData summary = reportService.getMomentReport(actor, momentId);
        User viewer = accessScope.loadActor(actor);
        byte[] content = pdfWriter.moment(summary, visibleGroups(viewer, momentId), gradeScaleLoader.load(),
                viewer.getFullName());
        return file(content, "informe-" + momentName(summary) + ".pdf", PDF);
    }

    @Override
    public FileDownload groupPdf(AuthenticatedUser actor, Long groupId) {
        GroupReportData data = reportService.getGroupReport(actor, groupId);
        String name = accessScope.loadActor(actor).getFullName();
        byte[] content = pdfWriter.group(data, gradeScaleLoader.load(), name);
        String fileName = "acta-" + ExportFormat.slug(data.group().name()) + "-" + data.program().code() + "-"
                + data.period().code() + "-momento-" + data.moment().momentNumber() + ".pdf";
        return file(content, fileName, PDF);
    }

    @Override
    public FileDownload individualPdf(AuthenticatedUser actor, Long momentId, Long studentId) {
        IndividualReportData data = reportService.getIndividualReport(actor, momentId, studentId);
        String name = accessScope.loadActor(actor).getFullName();
        byte[] content = pdfWriter.individual(data, gradeScaleLoader.load(), name);
        String student = data.student().studentCode() != null
                ? data.student().studentCode()
                : ExportFormat.slug(data.student().fullName());
        String fileName = "boletin-" + student + "-" + data.period().code() + "-momento-"
                + data.moment().momentNumber() + ".pdf";
        return file(content, fileName, PDF);
    }

    /** Reportes de los grupos del momento que el usuario puede ver, en orden de nombre. */
    private List<GroupReportData> visibleGroups(User viewer, Long momentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        return projectGroupRepository.findByMomentIdOrderByName(momentId).stream()
                .filter(group -> ReportServiceImpl.canSeeGroupReport(viewer, group))
                .map(group -> new GroupReport(now, group, today).generate())
                .toList();
    }

    private static String momentName(MomentReportData summary) {
        return summary.program().code() + "-" + summary.period().code() + "-momento-"
                + summary.moment().momentNumber();
    }

    private static FileDownload file(byte[] content, String fileName, String contentType) {
        return new FileDownload(new ByteArrayResource(content), ExportFormat.slugFileName(fileName), contentType,
                content.length);
    }
}
