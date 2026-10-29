package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.ComparativeReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.ComparativeReport;
import co.edu.unicesar.sierap.entity.GlobalParameter;
import co.edu.unicesar.sierap.entity.GradeScale;
import co.edu.unicesar.sierap.entity.GroupReport;
import co.edu.unicesar.sierap.entity.IndividualReport;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.MomentReport;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.GlobalParameterRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ReportService} (Etapa 7).
 *
 * <p>Aplica las reglas de alcance de acceso por rol y delega el cálculo de estadísticas y métricas
 * al modelo de dominio mediante la jerarquía polimórfica de {@link co.edu.unicesar.sierap.entity.Report}.</p>
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    public static final String PARAM_PASSING_PERCENTAGE = "report.passing-percentage";
    public static final String PARAM_GRADE_BANDS = "report.grade-bands";

    private final MomentRepository momentRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final ProgramRepository programRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final UserRepository userRepository;
    private final GlobalParameterRepository globalParameterRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Construye el servicio de reportes.
     *
     * @param momentRepository          repositorio de momentos
     * @param projectGroupRepository    repositorio de grupos
     * @param programRepository         repositorio de programas
     * @param academicPeriodRepository  repositorio de periodos académicos
     * @param userRepository            repositorio de usuarios
     * @param globalParameterRepository repositorio de parámetros globales
     * @param accessScope               control de alcance de acceso
     * @param clock                     reloj inyectable de la aplicación
     */
    public ReportServiceImpl(
            MomentRepository momentRepository,
            ProjectGroupRepository projectGroupRepository,
            ProgramRepository programRepository,
            AcademicPeriodRepository academicPeriodRepository,
            UserRepository userRepository,
            GlobalParameterRepository globalParameterRepository,
            AccessScope accessScope,
            Clock clock) {
        this.momentRepository = momentRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.programRepository = programRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.userRepository = userRepository;
        this.globalParameterRepository = globalParameterRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public IndividualReportData getIndividualReport(AuthenticatedUser actor, Long momentId, Long studentId) {
        User viewer = accessScope.loadActor(actor);
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", momentId));

        Student targetStudent;
        if (viewer instanceof Student studentViewer) {
            if (studentId != null && !studentId.equals(studentViewer.getId())) {
                throw new ForbiddenAccessException("Solo puede consultar su propio reporte individual.");
            }
            LocalDate gradingEnd = moment.getGradingEnd();
            boolean isClosed = moment.getStatus() == MomentStatus.LAUNCHED && gradingEnd != null
                    && LocalDate.now(clock).isAfter(gradingEnd);
            if (!isClosed) {
                throw new BusinessRuleException("Los resultados de este momento aún no están disponibles.");
            }
            targetStudent = studentViewer;
        } else {
            if (studentId == null) {
                throw new BusinessRuleException("Debe especificar el estudiante a consultar.");
            }
            User user = userRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estudiante", studentId));
            if (!(user instanceof Student student)) {
                throw new ResourceNotFoundException("Estudiante", studentId);
            }
            targetStudent = student;

            Long facultyId = moment.getProgram().getFaculty().getId();
            if (viewer instanceof Admin admin) {
                accessScope.requireFaculty(admin, facultyId);
            } else if (viewer instanceof Teacher teacher) {
                accessScope.requireFaculty(teacher, facultyId);
            } else if (!(viewer instanceof SuperAdmin)) {
                throw new ForbiddenAccessException("No tiene permisos para consultar este reporte.");
            }
        }

        ProjectGroup group = projectGroupRepository.findByMomentIdAndMembersId(momentId, targetStudent.getId())
                .orElseThrow(() -> new BusinessRuleException("El estudiante no pertenece a ningún grupo de este momento."));

        if (viewer instanceof Teacher teacher && !group.isEnabledBy(teacher)) {
            throw new ForbiddenAccessException("Solo puede consultar reportes de estudiantes en los grupos que usted habilitó.");
        }

        IndividualReport report = new IndividualReport(
                LocalDateTime.now(clock),
                targetStudent,
                group,
                LocalDate.now(clock)
        );
        return report.generate();
    }

    @Override
    public GroupReportData getGroupReport(AuthenticatedUser actor, Long groupId) {
        User viewer = accessScope.loadActor(actor);
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", groupId));

        if (!canSeeGroupReport(viewer, group)) {
            throw new ForbiddenAccessException(
                    "Solo el docente del grupo, el admin de su facultad y el super_admin pueden consultar su reporte.");
        }

        GroupReport report = new GroupReport(
                LocalDateTime.now(clock),
                group,
                LocalDate.now(clock)
        );
        return report.generate();
    }

    @Override
    public MomentReportData getMomentReport(AuthenticatedUser actor, Long momentId) {
        User viewer = accessScope.loadActor(actor);
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", momentId));

        Long facultyId = moment.getProgram().getFaculty().getId();
        if (viewer instanceof Admin admin) {
            accessScope.requireFaculty(admin, facultyId);
        } else if (viewer instanceof Teacher teacher) {
            accessScope.requireFaculty(teacher, facultyId);
        } else if (!(viewer instanceof SuperAdmin)) {
            throw new ForbiddenAccessException("No tiene permisos para consultar el reporte consolidado del momento.");
        }

        List<ProjectGroup> groups = projectGroupRepository.findByMomentIdOrderByName(momentId);
        GradeScale gradeScale = loadGradeScale();
        MomentReport report = new MomentReport(
                LocalDateTime.now(clock),
                moment,
                groups,
                LocalDate.now(clock),
                gradeScale
        );
        return report.generate();
    }

    @Override
    public ComparativeReportData getComparativeReport(
            AuthenticatedUser actor,
            Long programId,
            int momentNumber,
            Long periodAId,
            Long periodBId) {
        if (Objects.equals(periodAId, periodBId)) {
            throw new BusinessRuleException("Debe elegir dos periodos académicos distintos para comparar.");
        }

        User viewer = accessScope.loadActor(actor);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa", programId));

        Long facultyId = program.getFaculty().getId();
        if (viewer instanceof Admin admin) {
            accessScope.requireFaculty(admin, facultyId);
        } else if (viewer instanceof Teacher teacher) {
            accessScope.requireFaculty(teacher, facultyId);
        } else if (!(viewer instanceof SuperAdmin)) {
            throw new ForbiddenAccessException("No tiene permisos para consultar reportes comparativos.");
        }

        AcademicPeriod periodA = academicPeriodRepository.findById(periodAId)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo", periodAId));
        AcademicPeriod periodB = academicPeriodRepository.findById(periodBId)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo", periodBId));

        Moment momentA = momentRepository.findByProgramIdAndPeriodIdAndNumber(programId, periodAId, momentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El programa no tiene un momento " + momentNumber + " en el periodo " + periodA.getCode() + "."));
        Moment momentB = momentRepository.findByProgramIdAndPeriodIdAndNumber(programId, periodBId, momentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El programa no tiene un momento " + momentNumber + " en el periodo " + periodB.getCode() + "."));

        List<ProjectGroup> groupsA = projectGroupRepository.findByMomentIdOrderByName(momentA.getId());
        List<ProjectGroup> groupsB = projectGroupRepository.findByMomentIdOrderByName(momentB.getId());
        GradeScale gradeScale = loadGradeScale();

        ComparativeReport report = new ComparativeReport(
                LocalDateTime.now(clock),
                momentA,
                groupsA,
                momentB,
                groupsB,
                LocalDate.now(clock),
                gradeScale
        );
        return report.generate();
    }

    private GradeScale loadGradeScale() {
        GlobalParameter passingParam = globalParameterRepository.findByKey(PARAM_PASSING_PERCENTAGE)
                .orElseThrow(() -> new BusinessRuleException(
                        "Falta configurar el parámetro global '" + PARAM_PASSING_PERCENTAGE + "'"));
        GlobalParameter bandsParam = globalParameterRepository.findByKey(PARAM_GRADE_BANDS)
                .orElseThrow(() -> new BusinessRuleException(
                        "Falta configurar el parámetro global '" + PARAM_GRADE_BANDS + "'"));

        return GradeScale.of(passingParam.getValue(), bandsParam.getValue());
    }

    private static boolean canSeeGroupReport(User viewer, ProjectGroup group) {
        if (viewer instanceof SuperAdmin) {
            return true;
        }
        if (viewer instanceof Admin admin) {
            return Objects.equals(admin.getFaculty().getId(), group.getMoment().getProgram().getFaculty().getId());
        }
        return viewer instanceof Teacher teacher && group.isEnabledBy(teacher);
    }
}
