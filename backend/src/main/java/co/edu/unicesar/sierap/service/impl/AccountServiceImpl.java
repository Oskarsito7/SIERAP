package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.request.NewAdminRequest;
import co.edu.unicesar.sierap.dto.request.NewExternalEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.request.NewTeacherRequest;
import co.edu.unicesar.sierap.dto.request.UpdateUserRequest;
import co.edu.unicesar.sierap.dto.response.StudentResponse;
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
import co.edu.unicesar.sierap.service.PasswordLinkService;
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
                              AccountValidator accountValidator, Clock clock) {
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
        this.clock = clock;
    }

    @Override
    public UserSummaryResponse createAdmin(AuthenticatedUser actor, NewAdminRequest request) {
        User creator = accessScope.loadActor(actor);
        Faculty faculty = findFaculty(request.facultyId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        Admin admin = new Admin(request.firstNames().trim(), request.lastNames().trim(), request.documentType(),
                request.documentNumber().trim(), request.email(), null, now(), faculty);
        return UserMapper.toSummary(register(admin, creator));
    }

    @Override
    public UserSummaryResponse createTeacher(AuthenticatedUser actor, NewTeacherRequest request) {
        User creator = accessScope.loadActor(actor);
        Faculty faculty = findFaculty(request.facultyId());
        accessScope.requireFaculty(creator, faculty.getId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        Teacher teacher = new Teacher(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), request.email(), null, now(), faculty);
        return UserMapper.toSummary(register(teacher, creator));
    }

    @Override
    public StudentResponse createStudent(AuthenticatedUser actor, NewStudentRequest request) {
        User creator = accessScope.loadActor(actor);
        Program program = findProgram(request.programId());
        accessScope.requireFaculty(creator, program.getFaculty().getId());
        accountValidator.requireNewUser(request.email(), request.documentNumber());
        accountValidator.requireNewStudentCode(request.studentCode());
        Student student = new Student(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), request.email(), null, now(),
                request.studentCode().trim(), request.currentSemester(), program);
        return UserMapper.toStudent(register(student, creator), accessScope.canManage(creator, student));
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
        return UserMapper.toSummary(register(evaluator, creator));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listAdmins(Long facultyId) {
        findFaculty(facultyId);
        return adminRepository.findByFacultyId(facultyId).stream().map(UserMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listAdmins(Long facultyId) {
        findFaculty(facultyId);
        return adminRepository.findByFacultyId(facultyId).stream().map(UserMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listTeachers(AuthenticatedUser actor, Long facultyId) {
        accessScope.requireFaculty(accessScope.loadActor(actor), findFaculty(facultyId).getId());
        return teacherRepository.findByFacultyId(facultyId).stream().map(UserMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(AuthenticatedUser actor, Long programId, Long withoutGroupInMoment) {
        User viewer = accessScope.loadActor(actor);
        Program program = findProgram(programId);
        accessScope.requireFaculty(viewer, program.getFaculty().getId());
        List<Student> students = withoutGroupInMoment == null
                ? studentRepository.findByProgramIdOrderByLastNamesAscFirstNamesAsc(programId)
                : withoutGroupIn(program, withoutGroupInMoment);
        return students.stream()
                .map(student -> UserMapper.toStudent(student, accessScope.canManage(viewer, student)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listExternalEvaluators(AuthenticatedUser actor) {
        User viewer = accessScope.loadActor(actor);
        List<ExternalEvaluator> evaluators = viewer instanceof SuperAdmin
                ? externalEvaluatorRepository.findAll(Sort.by("lastNames"))
                : externalEvaluatorRepository.findByCreatedByIdOrderByLastNamesAsc(viewer.getId());
        return evaluators.stream().map(UserMapper::toSummary).toList();
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
        if (Objects.equals(actor.id(), userId) && !request.active()) {
            throw new BusinessRuleException("No puede desactivar su propia cuenta.");
        }
        User target = findManageableUser(actor, userId);
        target.setActive(request.active());
        if (!request.active()) {
            refreshTokenRepository.deleteAllByUserId(target.getId());
        }
        return UserMapper.toSummary(target);
    }

    private <T extends User> T register(T user, User creator) {
        user.setCreatedBy(creator);
        userRepository.save(user);
        passwordLinkService.sendAccountSetupLink(user);
        return user;
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
