package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.dto.response.SubmissionHistoryResponse;
import co.edu.unicesar.sierap.dto.response.SubmissionResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Evaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
import co.edu.unicesar.sierap.entity.Submission.StoredFile;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.SubmissionMapper;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.SubmissionRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.FileStorageService;
import co.edu.unicesar.sierap.service.SubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación de {@link SubmissionService}.
 *
 * <p>Los archivos se validan ({@link SubmissionFileValidator}) y se guardan en disco
 * antes de registrar la versión en la base de datos; si la transacción se revierte,
 * se borran para no dejar archivos huérfanos. La fila del grupo se bloquea durante la
 * carga para que dos integrantes que suben a la vez obtengan versiones consecutivas.</p>
 */
@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    /** Tipo de contenido con el que se descarga cada extensión. */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".zip", "application/zip",
            ".pdf", "application/pdf",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final ProjectGroupRepository projectGroupRepository;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el service con sus dependencias.
     *
     * @param projectGroupRepository grupos
     * @param submissionRepository   entregas
     * @param fileStorageService     archivos en disco
     * @param accessScope            carga del usuario autenticado
     * @param clock                  reloj de la aplicación
     */
    public SubmissionServiceImpl(ProjectGroupRepository projectGroupRepository,
                                 SubmissionRepository submissionRepository, FileStorageService fileStorageService,
                                 AccessScope accessScope, Clock clock) {
        this.projectGroupRepository = projectGroupRepository;
        this.submissionRepository = submissionRepository;
        this.fileStorageService = fileStorageService;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public SubmissionResponse upload(AuthenticatedUser actor, Long groupId, MultipartFile zip,
                                     MultipartFile presentation) {
        User uploader = accessScope.loadActor(actor);
        ProjectGroup group = projectGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", groupId));
        if (!(uploader instanceof Student student) || !group.isMember(student)) {
            throw new ForbiddenAccessException("Solo los integrantes del grupo pueden subir entregas.");
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        LocalDate today = now.toLocalDate();
        requireSubmissionOpen(group.getMoment(), today);

        String zipExtension = SubmissionFileValidator.validateZip(zip);
        boolean withPresentation = presentation != null && !presentation.isEmpty();
        String presentationExtension = withPresentation
                ? SubmissionFileValidator.validatePresentation(presentation)
                : null;

        int version = group.nextSubmissionVersion();
        String directory = "groups/" + group.getId() + "/v" + version;
        List<String> storedPaths = new ArrayList<>();
        deleteFilesOnRollback(storedPaths);

        StoredFile storedZip = fileStorageService.store(zip, directory, zipExtension);
        storedPaths.add(storedZip.path());
        Submission submission = new Submission(version, storedZip, now, isLate(group, today));
        if (withPresentation) {
            StoredFile storedPresentation = fileStorageService.store(presentation, directory, presentationExtension);
            storedPaths.add(storedPresentation.path());
            submission.attachPresentation(storedPresentation);
        }
        group.addSubmission(submission);
        submissionRepository.save(submission);
        return SubmissionMapper.toResponse(submission, group.getSubmissionToGrade());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionHistoryResponse history(AuthenticatedUser actor, Long groupId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", groupId));
        requireCanSeeSubmissions(accessScope.loadActor(actor), group);
        return SubmissionMapper.toHistory(group);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload downloadZip(AuthenticatedUser actor, Long submissionId) {
        Submission submission = findVisibleSubmission(actor, submissionId);
        return download(submission.getZipFilePath(), submission.getZipFileName(), submission.getZipSize());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload downloadPresentation(AuthenticatedUser actor, Long submissionId) {
        Submission submission = findVisibleSubmission(actor, submissionId);
        if (!submission.hasPresentation()) {
            throw new ResourceNotFoundException("Esta versión de la entrega no tiene presentación.");
        }
        return download(submission.getPresentationPath(), submission.getPresentationFileName(),
                submission.getPresentationSize());
    }

    // ---------------------------------------------------------------- reglas comunes

    /**
     * Quién ve las entregas de un grupo: sus integrantes, su docente, sus evaluadores
     * externos asignados, el admin de la facultad y el super_admin. Los estudiantes de
     * otros grupos no las ven.
     */
    private static void requireCanSeeSubmissions(User viewer, ProjectGroup group) {
        boolean allowed;
        if (viewer instanceof SuperAdmin) {
            allowed = true;
        } else if (viewer instanceof Admin admin) {
            allowed = Objects.equals(admin.getFaculty().getId(), group.getMoment().getProgram().getFaculty().getId());
        } else if (viewer instanceof Student student) {
            allowed = group.isMember(student);
        } else {
            allowed = viewer instanceof Evaluator evaluator && evaluator.canEvaluate(group);
        }
        if (!allowed) {
            throw new ForbiddenAccessException("No tiene acceso a las entregas de este grupo.");
        }
    }

    private Submission findVisibleSubmission(AuthenticatedUser actor, Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega", submissionId));
        requireCanSeeSubmissions(accessScope.loadActor(actor), submission.getProjectGroup());
        return submission;
    }

    private FileDownload download(String path, String fileName, long size) {
        String extension = path.substring(path.lastIndexOf('.'));
        return new FileDownload(fileStorageService.load(path), fileName,
                CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"), size);
    }

    /** Solo se sube con el momento lanzado y dentro de la ventana de entregas (docs/07). */
    private static void requireSubmissionOpen(Moment moment, LocalDate today) {
        if (moment.isInSubmission(today)) {
            return;
        }
        if (moment.getStatus() != MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento todavía no se ha lanzado: aún no se reciben entregas.");
        }
        LocalDate start = moment.getSubmissionStart();
        if (start != null && today.isBefore(start)) {
            throw new BusinessRuleException("La ventana de entregas abre el " + start + ".");
        }
        throw new BusinessRuleException("La ventana de entregas cerró el " + moment.getSubmissionEnd() + ".");
    }

    /**
     * Tardía si hoy es después de la fecha límite del grupo o, si el docente no fijó
     * una, después del fin de la ventana de entregas.
     */
    private static boolean isLate(ProjectGroup group, LocalDate today) {
        LocalDate deadline = group.getSubmissionDeadline() != null
                ? group.getSubmissionDeadline()
                : group.getMoment().getSubmissionEnd();
        return deadline != null && today.isAfter(deadline);
    }

    /**
     * Si la transacción no termina confirmada (p. ej. otra carga tomó el mismo número
     * de versión), borra los archivos que alcanzó a guardar.
     */
    private void deleteFilesOnRollback(List<String> storedPaths) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storedPaths.forEach(fileStorageService::delete);
                }
            }
        });
    }
}
