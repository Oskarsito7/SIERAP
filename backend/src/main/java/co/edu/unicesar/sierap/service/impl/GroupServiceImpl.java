package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.request.AddMemberRequest;
import co.edu.unicesar.sierap.dto.request.AssignEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewGroupRequest;
import co.edu.unicesar.sierap.dto.request.ProjectTitleRequest;
import co.edu.unicesar.sierap.dto.request.UpdateGroupRequest;
import co.edu.unicesar.sierap.dto.response.GroupResponse;
import co.edu.unicesar.sierap.dto.response.StudentWithoutGroupResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Evaluator;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.GroupMapper;
import co.edu.unicesar.sierap.mapper.UserMapper;
import co.edu.unicesar.sierap.repository.EvaluationRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link GroupService}.
 *
 * <p>El alcance se decide por el tipo de usuario (polimorfismo con
 * {@link Evaluator#canEvaluate(ProjectGroup)} para docentes y evaluadores externos; el
 * estudiante ve los grupos de los momentos a los que puede acceder) y
 * las fechas con el {@code Clock} inyectado. Toda regla violada responde con una
 * excepción de SIERAP que {@code GlobalExceptionHandler} convierte en 403, 404 o 422.</p>
 */
@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    private final ProjectGroupRepository projectGroupRepository;
    private final MomentRepository momentRepository;
    private final StudentRepository studentRepository;
    private final ExternalEvaluatorRepository externalEvaluatorRepository;
    private final EvaluationRepository evaluationRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el service con sus dependencias.
     *
     * @param projectGroupRepository      grupos
     * @param momentRepository            momentos
     * @param studentRepository           estudiantes
     * @param externalEvaluatorRepository evaluadores externos
     * @param evaluationRepository        evaluaciones
     * @param accessScope                 reglas de alcance de las cuentas
     * @param clock                       reloj de la aplicación
     */
    public GroupServiceImpl(ProjectGroupRepository projectGroupRepository, MomentRepository momentRepository,
                            StudentRepository studentRepository,
                            ExternalEvaluatorRepository externalEvaluatorRepository,
                            EvaluationRepository evaluationRepository, AccessScope accessScope, Clock clock) {
        this.projectGroupRepository = projectGroupRepository;
        this.momentRepository = momentRepository;
        this.studentRepository = studentRepository;
        this.externalEvaluatorRepository = externalEvaluatorRepository;
        this.evaluationRepository = evaluationRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> list(AuthenticatedUser actor, Long momentId) {
        User viewer = accessScope.loadActor(actor);
        List<ProjectGroup> groups;
        if (viewer instanceof Teacher teacher) {
            groups = momentId == null
                    ? projectGroupRepository.findByTeacherIdOrderByName(teacher.getId())
                    : projectGroupRepository.findByTeacherIdAndMomentIdOrderByName(teacher.getId(), momentId);
        } else if (viewer instanceof ExternalEvaluator evaluator) {
            groups = momentId == null
                    ? projectGroupRepository.findByEvaluatorsIdOrderByName(evaluator.getId())
                    : projectGroupRepository.findByEvaluatorsIdAndMomentIdOrderByName(evaluator.getId(), momentId);
        } else if (viewer instanceof Student student) {
            if (momentId == null) {
                throw new BusinessRuleException("Indique el momento del que quiere ver los grupos.");
            }
            Moment moment = findMoment(momentId);
            if (!canAccessMoment(student, moment)) {
                throw new ForbiddenAccessException("No tiene acceso a los grupos de este momento.");
            }
            groups = projectGroupRepository.findByMomentIdOrderByName(momentId);
        } else if (viewer instanceof Admin || viewer instanceof SuperAdmin) {
            if (momentId == null) {
                throw new BusinessRuleException("Indique el momento del que quiere ver los grupos.");
            }
            Moment moment = findMoment(momentId);
            accessScope.requireFaculty(viewer, facultyOf(moment));
            groups = projectGroupRepository.findByMomentIdOrderByName(momentId);
        } else {
            throw new ForbiddenAccessException("No tiene acceso a los grupos.");
        }
        return groups.stream().map(group -> GroupMapper.toResponse(group, viewer)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse get(AuthenticatedUser actor, Long id) {
        User viewer = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(id);
        if (!canView(viewer, group)) {
            throw new ForbiddenAccessException("No tiene acceso a este grupo.");
        }
        return GroupMapper.toResponse(group, viewer);
    }

    @Override
    public GroupResponse create(AuthenticatedUser actor, NewGroupRequest request) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        Moment moment = findMoment(request.momentId());
        if (!Objects.equals(facultyOf(moment), teacher.getFaculty().getId())) {
            throw new ForbiddenAccessException("Solo puede habilitar grupos en momentos de su facultad.");
        }
        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento ya fue lanzado: no se pueden habilitar grupos nuevos.");
        }
        String name = request.name().trim();
        requireUniqueName(moment, teacher, name);
        ProjectGroup group = projectGroupRepository.save(new ProjectGroup(moment, teacher, name));
        return GroupMapper.toResponse(group, teacher);
    }

    @Override
    public GroupResponse join(AuthenticatedUser actor, Long id) {
        Student student = requireStudent(accessScope.loadActor(actor));
        // Se bloquea la fila del grupo para que dos inscripciones simultáneas no pasen
        // a la vez la validación del cupo.
        ProjectGroup group = projectGroupRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", id));
        Moment moment = group.getMoment();
        if (!canAccessMoment(student, moment)) {
            throw new ForbiddenAccessException("No puede inscribirse en los grupos de este momento.");
        }
        requireGroupFormationOpen(moment, today());
        if (group.isMember(student)) {
            throw new BusinessRuleException("Ya es integrante de este grupo.");
        }
        if (group.isFull()) {
            throw new BusinessRuleException("El grupo ya tiene el máximo de " + moment.getMaxMembers()
                    + " integrantes.");
        }
        // Cambio de grupo en un solo paso: sale del grupo anterior del mismo momento.
        projectGroupRepository.findByMomentIdAndMembersId(moment.getId(), student.getId())
                .ifPresent(previous -> previous.removeMember(student));
        group.addMember(student);
        return GroupMapper.toResponse(group, student);
    }

    @Override
    public void leave(AuthenticatedUser actor, Long id) {
        Student student = requireStudent(accessScope.loadActor(actor));
        ProjectGroup group = findGroup(id);
        if (!group.isMember(student)) {
            throw new BusinessRuleException("No es integrante de este grupo.");
        }
        requireGroupFormationOpen(group.getMoment(), today());
        group.removeMember(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> myGroups(AuthenticatedUser actor) {
        Student student = requireStudent(accessScope.loadActor(actor));
        return projectGroupRepository.findByMembersIdOrderByMomentPeriodStartDateDescMomentNumberDesc(student.getId())
                .stream()
                .map(group -> GroupMapper.toResponse(group, student))
                .toList();
    }

    @Override
    public GroupResponse update(AuthenticatedUser actor, Long id, UpdateGroupRequest request) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        ProjectGroup group = findOwnGroup(teacher, id);
        Moment moment = group.getMoment();
        LocalDate today = today();

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equals(group.getName())) {
                requireConfiguration(moment, "El nombre del grupo solo se puede cambiar antes de lanzar el momento.");
                requireUniqueName(moment, teacher, name);
                group.setName(name);
            }
        }
        if (request.submissionDeadline() != null || request.evaluationDate() != null) {
            requireGradingNotClosed(moment, today, "cambiar las fechas del grupo");
        }
        if (request.submissionDeadline() != null) {
            requireDateInWindow(request.submissionDeadline(), today, moment.getSubmissionStart(),
                    moment.getSubmissionEnd(), "La fecha límite de entrega", "entregas");
            group.setSubmissionDeadline(request.submissionDeadline());
        }
        if (request.evaluationDate() != null) {
            requireDateInWindow(request.evaluationDate(), today, moment.getGradingStart(), moment.getGradingEnd(),
                    "La fecha de evaluación", "calificación");
            group.setEvaluationDate(request.evaluationDate());
        }
        return GroupMapper.toResponse(group, teacher);
    }

    @Override
    public void delete(AuthenticatedUser actor, Long id) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        ProjectGroup group = findOwnGroup(teacher, id);
        requireConfiguration(group.getMoment(), "El momento ya fue lanzado: el grupo no se puede eliminar.");
        if (!group.getMembers().isEmpty()) {
            throw new BusinessRuleException("El grupo tiene integrantes: retírelos antes de eliminarlo.");
        }
        projectGroupRepository.delete(group);
    }

    @Override
    public GroupResponse updateProjectTitle(AuthenticatedUser actor, Long id, ProjectTitleRequest request) {
        User viewer = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(id);
        boolean isTeacher = viewer instanceof Teacher teacher && group.isEnabledBy(teacher);
        boolean isMember = viewer instanceof Student student && group.isMember(student);
        if (!isTeacher && !isMember) {
            throw new ForbiddenAccessException("Solo los integrantes y el docente del grupo pueden cambiar el título.");
        }
        LocalDate submissionEnd = group.getMoment().getSubmissionEnd();
        if (submissionEnd != null && today().isAfter(submissionEnd)) {
            throw new BusinessRuleException("La ventana de entregas cerró el " + submissionEnd
                    + ": el título del proyecto ya no se puede cambiar.");
        }
        group.setProjectTitle(request.projectTitle().trim());
        return GroupMapper.toResponse(group, viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentWithoutGroupResponse> studentsWithoutGroup(AuthenticatedUser actor, Long momentId) {
        User viewer = accessScope.loadActor(actor);
        Moment moment = findMoment(momentId);
        accessScope.requireFaculty(viewer, facultyOf(moment));
        return studentRepository.findWithoutGroup(moment.getProgram().getId(), moment.getSemesterFrom(),
                        moment.getSemesterTo(), moment.getId())
                .stream()
                .map(GroupMapper::toStudentWithoutGroup)
                .toList();
    }

    @Override
    public GroupResponse addMember(AuthenticatedUser actor, Long id, AddMemberRequest request) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        // Se bloquea la fila del grupo para que dos cambios simultáneos no se pisen.
        ProjectGroup group = projectGroupRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", id));
        requireOwner(teacher, group);
        Moment moment = group.getMoment();
        requireGradingNotClosed(moment, today(), "agregar estudiantes");
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", request.studentId()));
        if (!Objects.equals(student.getProgram().getId(), moment.getProgram().getId())) {
            throw new BusinessRuleException("El estudiante no es del programa del momento.");
        }
        projectGroupRepository.findByMomentIdAndMembersId(moment.getId(), student.getId()).ifPresent(current -> {
            throw new BusinessRuleException("El estudiante ya tiene grupo en este momento: " + current.getName()
                    + ".");
        });
        group.addMember(student);
        return GroupMapper.toResponse(group, teacher);
    }

    @Override
    public void removeMember(AuthenticatedUser actor, Long id, Long studentId) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        ProjectGroup group = findOwnGroup(teacher, id);
        Student student = group.getMembers().stream()
                .filter(member -> Objects.equals(member.getId(), studentId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El estudiante no es integrante de este grupo."));
        if (evaluationRepository.existsByProjectGroupIdAndStatus(group.getId(), EvaluationStatus.SUBMITTED)) {
            throw new BusinessRuleException(
                    "El grupo ya tiene una evaluación enviada: no se pueden retirar integrantes.");
        }
        group.removeMember(student);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse lookupExternalEvaluator(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) {
            throw new BusinessRuleException("Escriba el correo o el número de documento del evaluador.");
        }
        return externalEvaluatorRepository
                .findActiveByEmailOrDocument(value.toLowerCase(Locale.ROOT), value)
                .map(UserMapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un evaluador externo activo con ese correo o documento."));
    }

    @Override
    public GroupResponse assignEvaluator(AuthenticatedUser actor, Long id, AssignEvaluatorRequest request) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        ProjectGroup group = findOwnGroup(teacher, id);
        requireGradingNotClosed(group.getMoment(), today(), "asignar evaluadores");
        ExternalEvaluator evaluator = externalEvaluatorRepository.findById(request.evaluatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Evaluador externo", request.evaluatorId()));
        if (!evaluator.isActive()) {
            throw new BusinessRuleException("El evaluador está desactivado.");
        }
        if (group.hasEvaluator(evaluator)) {
            throw new BusinessRuleException("El evaluador ya está asignado a este grupo.");
        }
        group.addEvaluator(evaluator);
        return GroupMapper.toResponse(group, teacher);
    }

    @Override
    public void removeEvaluator(AuthenticatedUser actor, Long id, Long evaluatorId) {
        Teacher teacher = requireTeacher(accessScope.loadActor(actor));
        ProjectGroup group = findOwnGroup(teacher, id);
        requireGradingNotClosed(group.getMoment(), today(), "retirar evaluadores");
        ExternalEvaluator evaluator = group.getEvaluators().stream()
                .filter(assigned -> Objects.equals(assigned.getId(), evaluatorId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El evaluador no está asignado a este grupo."));
        if (evaluationRepository.existsByProjectGroupIdAndEvaluatorId(group.getId(), evaluatorId)) {
            throw new BusinessRuleException(
                    "El evaluador ya empezó a calificar este grupo: no se puede retirar.");
        }
        group.removeEvaluator(evaluator);
    }

    // ---------------------------------------------------------------- reglas comunes

    /**
     * Quién ve un grupo: el super_admin, el admin de la facultad, los evaluadores del
     * grupo (su docente y los externos asignados) y los estudiantes que pueden acceder
     * a su momento, cada uno con su propia regla.
     */
    private boolean canView(User viewer, ProjectGroup group) {
        if (viewer instanceof SuperAdmin) {
            return true;
        }
        if (viewer instanceof Student student) {
            return group.isMember(student) || canAccessMoment(student, group.getMoment());
        }
        if (viewer instanceof Admin admin) {
            return Objects.equals(admin.getFaculty().getId(), facultyOf(group.getMoment()));
        }
        return viewer instanceof Evaluator evaluator && evaluator.canEvaluate(group);
    }

    /**
     * Momento al que accede un estudiante: el de su programa cuyo rango de semestres
     * incluye su semestre actual o, aunque esté fuera del rango, uno en el que ya tiene
     * grupo (porque el docente lo agregó).
     */
    private boolean canAccessMoment(Student student, Moment moment) {
        if (!Objects.equals(student.getProgram().getId(), moment.getProgram().getId())) {
            return false;
        }
        int semester = student.getCurrentSemester();
        return semester >= moment.getSemesterFrom() && semester <= moment.getSemesterTo()
                || projectGroupRepository.existsByMomentIdAndMembersId(moment.getId(), student.getId());
    }

    /** Inscribirse, cambiarse o salirse solo se puede durante la conformación (docs/07). */
    private static void requireGroupFormationOpen(Moment moment, LocalDate today) {
        if (moment.isInGroupFormation(today)) {
            return;
        }
        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento ya fue lanzado: los grupos ya no se pueden cambiar.");
        }
        LocalDate start = moment.getGroupFormationStart();
        LocalDate end = moment.getGroupFormationEnd();
        if (start == null || end == null) {
            throw new BusinessRuleException(
                    "El momento todavía no tiene definida la ventana de conformación de grupos.");
        }
        if (today.isBefore(start)) {
            throw new BusinessRuleException("La conformación de grupos abre el " + start + ".");
        }
        throw new BusinessRuleException("La ventana de conformación de grupos cerró el " + end + ".");
    }

    private Student requireStudent(User user) {
        if (user instanceof Student student) {
            return student;
        }
        throw new ForbiddenAccessException("Solo los estudiantes se inscriben en los grupos.");
    }

    private Teacher requireTeacher(User user) {
        if (user instanceof Teacher teacher) {
            return teacher;
        }
        throw new ForbiddenAccessException("Solo los docentes gestionan sus grupos.");
    }

    private ProjectGroup findOwnGroup(Teacher teacher, Long id) {
        ProjectGroup group = findGroup(id);
        requireOwner(teacher, group);
        return group;
    }

    private void requireOwner(Teacher teacher, ProjectGroup group) {
        if (!group.isEnabledBy(teacher)) {
            throw new ForbiddenAccessException("Solo el docente que habilitó el grupo puede modificarlo.");
        }
    }

    private void requireUniqueName(Moment moment, Teacher teacher, String name) {
        if (projectGroupRepository.existsByMomentIdAndTeacherIdAndNameIgnoreCase(moment.getId(), teacher.getId(),
                name)) {
            throw new BusinessRuleException("Ya tiene un grupo llamado \"" + name + "\" en este momento.");
        }
    }

    private static void requireConfiguration(Moment moment, String message) {
        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException(message);
        }
    }

    /** Después de la ventana de calificación ya no se cambia nada del grupo (docs/07). */
    private static void requireGradingNotClosed(Moment moment, LocalDate today, String action) {
        LocalDate gradingEnd = moment.getGradingEnd();
        if (gradingEnd != null && today.isAfter(gradingEnd)) {
            throw new BusinessRuleException("La ventana de calificación cerró el " + gradingEnd
                    + ": ya no se puede " + action + ".");
        }
    }

    /** La fecha debe caer dentro de la ventana del momento y no puede ser anterior a hoy. */
    private static void requireDateInWindow(LocalDate date, LocalDate today, LocalDate start, LocalDate end,
                                            String field, String window) {
        if (start == null || end == null) {
            throw new BusinessRuleException(
                    "El momento todavía no tiene definida la ventana de " + window + ".");
        }
        if (date.isBefore(start) || date.isAfter(end)) {
            throw new BusinessRuleException(field + " debe estar dentro de la ventana de " + window
                    + " (del " + start + " al " + end + ").");
        }
        if (date.isBefore(today)) {
            throw new BusinessRuleException(field + " no puede ser anterior a hoy.");
        }
    }

    private ProjectGroup findGroup(Long id) {
        return projectGroupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grupo", id));
    }

    private Moment findMoment(Long id) {
        return momentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Momento", id));
    }

    private static Long facultyOf(Moment moment) {
        return moment.getProgram().getFaculty().getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
