package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import co.edu.unicesar.sierap.dto.request.NewAdminRequest;
import co.edu.unicesar.sierap.dto.request.NewExternalEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.request.NewTeacherRequest;
import co.edu.unicesar.sierap.dto.request.SetSemesterRequest;
import co.edu.unicesar.sierap.dto.request.UpdateUserRequest;
import co.edu.unicesar.sierap.dto.response.PagedResult;
import co.edu.unicesar.sierap.dto.response.StudentResponse;
import co.edu.unicesar.sierap.dto.response.UpdatedCountResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.UserMapper;
import co.edu.unicesar.sierap.repository.AdminRepository;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.RefreshTokenRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.TeacherRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AccountService;
import co.edu.unicesar.sierap.service.AuditService;
import co.edu.unicesar.sierap.service.PasswordLinkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link AccountService} (ADR-017).
 *
 * <p>El rol exigido por cada operación lo valida el controller con
 * {@code @PreAuthorize}; aquí se valida el <b>alcance</b> (la facultad del usuario)
 * con {@link AccessScope}. Toda cuenta nueva se crea sin contraseña y recibe por
 * correo el enlace para crearla.</p>
 */
@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    /** Orden de la lista de estudiantes: apellidos y nombres (el id desempata para paginar estable). */
    private static final Sort STUDENT_ORDER = Sort.by("lastNames", "firstNames", "id");

    /** Orden de la lista de evaluadores externos: apellidos (el id desempata). */
    private static final Sort EVALUATOR_ORDER = Sort.by("lastNames", "id");

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ExternalEvaluatorRepository externalEvaluatorRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final MomentRepository momentRepository;
    private final EvaluatorTypeRepository evaluatorTypeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordLinkService passwordLinkService;
    private final AccessScope accessScope;
    private final AccountValidator accountValidator;
    private final AuditService auditService;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository              repositorio de usuarios
     * @param adminRepository             repositorio de administradores
     * @param teacherRepository           repositorio de docentes
     * @param studentRepository           repositorio de estudiantes
     * @param externalEvaluatorRepository repositorio de evaluadores externos
     * @param facultyRepository           repositorio de facultades
     * @param programRepository           repositorio de programas
     * @param momentRepository            repositorio de momentos
     * @param evaluatorTypeRepository     catálogo de tipos de evaluador
     * @param refreshTokenRepository      repositorio de sesiones
     * @param passwordLinkService         enlaces por correo
     * @param accessScope                 reglas de alcance
     * @param accountValidator            validaciones de cuentas nuevas
     * @param auditService                bitácora de auditoría
     * @param clock                       reloj de la aplicación
     */
    public AccountServiceImpl(UserRepository userRepository, AdminRepository adminRepository,
                              TeacherRepository teacherRepository,
                              StudentRepository studentRepository,
                              ExternalEvaluatorRepository externalEvaluatorRepository,
                              FacultyRepository facultyRepository, ProgramRepository programRepository,
                              MomentRepository momentRepository,
                              EvaluatorTypeRepository evaluatorTypeRepository,
                              RefreshTokenRepository refreshTokenRepository,
                              PasswordLinkService passwordLinkService, AccessScope accessScope,
                              AccountValidator accountValidator, AuditService auditService, Clock clock) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.externalEvaluatorRepository = externalEvaluatorRepository;
        this.facultyRepository = facultyRepository;
        this.programRepository = programRepository;
        this.momentRepository = momentRepository;
        this.evaluatorTypeRepository = evaluatorTypeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordLinkService = passwordLinkService;
        this.accessScope = accessScope;
        this.accountValidator = accountValidator;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public UserSummaryResponse createAdmin(AuthenticatedUser actor, NewAdminRequest request) {
        User creator = accessScope.loadActor(actor);
        Faculty faculty = findFaculty(request.facultyId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        Admin admin = new Admin(request.firstNames().trim(), request.lastNames().trim(), request.documentType(),
                request.documentNumber().trim(), request.email(), null, now(), faculty);
        return UserMapper.toSummaryWithDocument(register(admin, creator, faculty.getId()));
    }

    @Override
    public UserSummaryResponse createTeacher(AuthenticatedUser actor, NewTeacherRequest request) {
        User creator = accessScope.loadActor(actor);
        Faculty faculty = findFaculty(request.facultyId());
        accessScope.requireFaculty(creator, faculty.getId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        Teacher teacher = new Teacher(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), request.email(), null, now(), faculty);
        return UserMapper.toSummaryWithDocument(register(teacher, creator, faculty.getId()));
    }

    @Override
    public StudentResponse createStudent(AuthenticatedUser actor, NewStudentRequest request) {
        User creator = accessScope.loadActor(actor);
        Student student = buildStudent(creator, request);
        register(student, creator, student.getProgram().getFaculty().getId());
        return UserMapper.toStudent(student, accessScope.canManage(creator, student));
    }

    @Override
    public StudentResponse createImportedStudent(AuthenticatedUser actor, NewStudentRequest request) {
        User creator = accessScope.loadActor(actor);
        Student student = buildStudent(creator, request);
        persistAndInvite(student, creator);
        return UserMapper.toStudent(student, accessScope.canManage(creator, student));
    }

    private Student buildStudent(User creator, NewStudentRequest request) {
        Program program = findProgram(request.programId());
        accessScope.requireFaculty(creator, program.getFaculty().getId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        accountValidator.requireNewStudentCode(request.studentCode());
        return new Student(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), request.email(), null, now(),
                request.studentCode().trim(), request.currentSemester(), program);
    }

    @Override
    public UserSummaryResponse createExternalEvaluator(AuthenticatedUser actor,
                                                       NewExternalEvaluatorRequest request) {
        User creator = accessScope.loadActor(actor);
        EvaluatorType type = evaluatorTypeRepository.findById(request.evaluatorTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evaluador", request.evaluatorTypeId()));
        if (!type.isActive()) {
            throw new BusinessRuleException("El tipo de evaluador " + type.getName() + " está desactivado.");
        }
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        ExternalEvaluator evaluator = new ExternalEvaluator(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), request.email(), null, now(), type);
        evaluator.setOrganization(request.organization());
        evaluator.setPosition(request.position());
        evaluator.setGraduationYear(request.graduationYear());
        if (request.graduationProgramId() != null) {
            evaluator.setGraduationProgram(findProgram(request.graduationProgramId()));
        }
        return UserMapper.toSummaryWithDocument(register(evaluator, creator, accessScope.facultyOf(creator)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listAdmins(Long facultyId) {
        findFaculty(facultyId);
        return adminRepository.findByFacultyId(facultyId).stream().map(UserMapper::toSummaryWithDocument).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listTeachers(AuthenticatedUser actor, Long facultyId) {
        accessScope.requireFaculty(accessScope.loadActor(actor), findFaculty(facultyId).getId());
        return teacherRepository.findByFacultyId(facultyId).stream().map(UserMapper::toSummaryWithDocument).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(AuthenticatedUser actor, Long programId, Long withoutGroupInMoment) {
        User viewer = accessScope.loadActor(actor);
        Program program = findProgram(programId);
        accessScope.requireFaculty(viewer, program.getFaculty().getId());
        List<Student> students = withoutGroupInMoment == null
                ? studentRepository.findByProgramId(programId, Pageable.unpaged(STUDENT_ORDER)).getContent()
                : withoutGroupIn(program, withoutGroupInMoment);
        return students.stream()
                .map(student -> UserMapper.toStudent(student, accessScope.canManage(viewer, student)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<StudentResponse> searchStudents(AuthenticatedUser actor, Long programId, String query,
                                                       Pageable pageable) {
        User viewer = accessScope.loadActor(actor);
        Program program = findProgram(programId);
        accessScope.requireFaculty(viewer, program.getFaculty().getId());
        Pageable sorted = Paging.sorted(pageable, STUDENT_ORDER);
        Page<Student> page = query == null || query.isBlank()
                ? studentRepository.findByProgramId(programId, sorted)
                : studentRepository.searchByProgram(programId, Paging.likePattern(query), sorted);
        return Paging.toResult(page,
                student -> UserMapper.toStudent(student, accessScope.canManage(viewer, student)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listExternalEvaluators(AuthenticatedUser actor) {
        return listExternalEvaluators(actor, Pageable.unpaged()).items();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserSummaryResponse> listExternalEvaluators(AuthenticatedUser actor, Pageable pageable) {
        User viewer = accessScope.loadActor(actor);
        Pageable sorted = Paging.sorted(pageable, EVALUATOR_ORDER);
        Page<ExternalEvaluator> page = viewer instanceof SuperAdmin
                ? externalEvaluatorRepository.findAll(sorted)
                : externalEvaluatorRepository.findByCreatedById(viewer.getId(), sorted);
        return Paging.toResult(page, UserMapper::toSummaryWithDocument);
    }

    @Override
    public void resendPasswordLink(AuthenticatedUser actor, Long userId) {
        User target = findManageableUser(actor, userId);
        if (target.hasPassword()) {
            passwordLinkService.sendPasswordResetLink(target);
        } else {
            passwordLinkService.sendAccountSetupLink(target);
        }
    }

    @Override
    public UserSummaryResponse updateUser(AuthenticatedUser actor, Long userId, UpdateUserRequest request) {
        if (request.isEmpty()) {
            throw new BusinessRuleException("No hay cambios para guardar.");
        }
        if (Objects.equals(actor.id(), userId) && Boolean.FALSE.equals(request.active())) {
            throw new BusinessRuleException("No puede desactivar su propia cuenta.");
        }
        User target = findManageableUser(actor, userId);
        boolean studentFields = request.studentCode() != null || request.currentSemester() != null;
        if (studentFields && !(target instanceof Student)) {
            throw new BusinessRuleException("El código estudiantil y el semestre solo aplican a estudiantes.");
        }
        accountValidator.requireAvailableForUser(userId, request.email(), request.documentNumber());
        if (request.studentCode() != null) {
            accountValidator.requireAvailableStudentCode(userId, request.studentCode());
        }

        boolean emailChanged = request.email() != null
                && !AccountValidator.normalizeEmail(request.email()).equals(target.getEmail());
        String name = target.getFullName();
        String previousFirstNames = target.getFirstNames();
        String previousLastNames = target.getLastNames();
        DocumentType previousDocumentType = target.getDocumentType();
        String previousDocumentNumber = target.getDocumentNumber();
        String previousEmail = target.getEmail();
        boolean previousActive = target.isActive();
        String previousCode = target instanceof Student student ? student.getStudentCode() : null;
        int previousSemester = target instanceof Student student ? student.getCurrentSemester() : 0;
        if (request.firstNames() != null) {
            target.setFirstNames(request.firstNames().trim());
        }
        if (request.lastNames() != null) {
            target.setLastNames(request.lastNames().trim());
        }
        if (request.documentType() != null) {
            target.setDocumentType(request.documentType());
        }
        if (request.documentNumber() != null) {
            target.setDocumentNumber(request.documentNumber().trim());
        }
        if (request.email() != null) {
            target.setEmail(request.email());
        }
        if (target instanceof Student student) {
            if (request.studentCode() != null) {
                student.setStudentCode(request.studentCode().trim());
            }
            if (request.currentSemester() != null) {
                student.setCurrentSemester(request.currentSemester());
            }
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }
        if (emailChanged || Boolean.FALSE.equals(request.active())) {
            refreshTokenRepository.deleteAllByUserId(target.getId());
        }

        List<String> changes = new ArrayList<>();
        if (!previousFirstNames.equals(target.getFirstNames()) || !previousLastNames.equals(target.getLastNames())) {
            changes.add("el nombre (de " + previousFirstNames + " " + previousLastNames + " a "
                    + target.getFullName() + ")");
        }
        if (previousDocumentType != target.getDocumentType()
                || !previousDocumentNumber.equals(target.getDocumentNumber())) {
            changes.add("el documento");
        }
        if (!previousEmail.equals(target.getEmail())) {
            changes.add("el correo (de " + previousEmail + " a " + target.getEmail() + ")");
        }
        if (target instanceof Student student) {
            if (!Objects.equals(previousCode, student.getStudentCode())) {
                changes.add("el código estudiantil (de " + previousCode + " a " + student.getStudentCode() + ")");
            }
            if (previousSemester != student.getCurrentSemester()) {
                changes.add("el semestre (de " + previousSemester + " a " + student.getCurrentSemester() + ")");
            }
        }
        Long facultyId = auditFaculty(target);
        if (!changes.isEmpty()) {
            auditService.record(AuditAction.ACCOUNT_UPDATED, "User", target.getId(), facultyId,
                    "Cambió " + String.join(", ", changes) + " de la cuenta de " + name + ".");
        }
        if (previousActive != target.isActive()) {
            auditService.record(target.isActive() ? AuditAction.ACCOUNT_ACTIVATED : AuditAction.ACCOUNT_DEACTIVATED,
                    "User", target.getId(), facultyId,
                    (target.isActive() ? "Activó" : "Desactivó") + " la cuenta de " + name + " ("
                            + target.getEmail() + ").");
        }
        return UserMapper.toSummaryWithDocument(target);
    }

    @Override
    public UpdatedCountResponse setSemester(AuthenticatedUser actor, SetSemesterRequest request) {
        if (new HashSet<>(request.studentIds()).size() != request.studentIds().size()) {
            throw new BusinessRuleException("La lista de estudiantes tiene ids repetidos.");
        }
        User manager = accessScope.loadActor(actor);
        List<User> users = userRepository.findAllById(request.studentIds());
        if (users.size() != request.studentIds().size()) {
            var found = users.stream().map(User::getId).collect(Collectors.toSet());
            Long missing = request.studentIds().stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Estudiante", missing);
        }
        List<Student> students = users.stream().map(user -> {
            if (user instanceof Student student) {
                return student;
            }
            throw new BusinessRuleException("La cuenta " + user.getFullName() + " no es de un estudiante.");
        }).toList();
        for (Student student : students) {
            if (!accessScope.canManage(manager, student)) {
                throw new ForbiddenAccessException("El estudiante " + student.getFullName()
                        + " está fuera de su alcance.");
            }
        }
        students.forEach(student -> student.setCurrentSemester(request.currentSemester()));
        List<Long> faculties = students.stream().map(this::auditFaculty).distinct().toList();
        auditService.record(AuditAction.SEMESTER_BULK_UPDATED, "Student", null,
                faculties.size() == 1 ? faculties.get(0) : null,
                "Cambió el semestre de " + students.size()
                        + (students.size() == 1 ? " estudiante" : " estudiantes") + " a "
                        + request.currentSemester() + ".");
        return new UpdatedCountResponse(students.size());
    }

    private <T extends User> T register(T user, User creator, Long facultyId) {
        persistAndInvite(user, creator);
        auditService.record(AuditAction.ACCOUNT_CREATED, "User", user.getId(), facultyId,
                "Creó la cuenta de " + roleLabel(user) + " " + user.getFullName() + " (" + user.getEmail() + ").");
        return user;
    }

    private void persistAndInvite(User user, User creator) {
        user.setCreatedBy(creator);
        userRepository.save(user);
        passwordLinkService.sendAccountSetupLink(user);
    }

    private static String roleLabel(User user) {
        if (user instanceof Admin) {
            return "administrador";
        }
        if (user instanceof Teacher) {
            return "docente";
        }
        if (user instanceof Student) {
            return "estudiante";
        }
        if (user instanceof ExternalEvaluator) {
            return "evaluador externo";
        }
        return "super administrador";
    }

    /** Facultad del objeto afectado para la bitácora; {@code null} si la cuenta no pertenece a una. */
    private Long auditFaculty(User user) {
        if (user instanceof Student student) {
            return student.getProgram().getFaculty().getId();
        }
        if (user instanceof ExternalEvaluator evaluator) {
            return evaluator.getCreatedBy() == null ? null : accessScope.facultyOf(evaluator.getCreatedBy());
        }
        return accessScope.facultyOf(user);
    }

    /**
     * Filtro "Sin grupo" de DO-08: la misma regla de {@code students-without-group}, para
     * que las dos pantallas muestren los mismos estudiantes.
     */
    private List<Student> withoutGroupIn(Program program, Long momentId) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", momentId));
        if (!Objects.equals(moment.getProgram().getId(), program.getId())) {
            throw new BusinessRuleException("El momento " + moment.getName() + " no es del programa "
                    + program.getName() + ".");
        }
        return studentRepository.findWithoutGroup(program.getId(), moment.getSemesterFrom(),
                moment.getSemesterTo(), moment.getId());
    }

    private User findManageableUser(AuthenticatedUser actor, Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        if (!accessScope.canManage(accessScope.loadActor(actor), target)) {
            throw new ForbiddenAccessException("Esta cuenta está fuera de su alcance.");
        }
        return target;
    }

    private Faculty findFaculty(Long facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Facultad", facultyId));
    }

    private Program findProgram(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa", programId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
