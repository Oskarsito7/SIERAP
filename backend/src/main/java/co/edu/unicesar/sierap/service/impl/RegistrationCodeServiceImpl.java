package co.edu.unicesar.sierap.service.impl;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.request.NewRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.response.RegistrationCodeResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.RegistrationCode;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.RegistrationCodeMapper;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.RegistrationCodeRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.TeacherRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.RegistrationCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link RegistrationCodeService} (ADR-017).
 *
 * <p>Los códigos tienen 8 caracteres de un alfabeto sin letras ni números que se
 * confundan al dictarlos (sin 0/O ni 1/I).</p>
 */
@Service
@Transactional
public class RegistrationCodeServiceImpl implements RegistrationCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();

    private final RegistrationCodeRepository registrationCodeRepository;
    private final TeacherRepository teacherRepository;
    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param registrationCodeRepository repositorio de códigos
     * @param teacherRepository          repositorio de docentes
     * @param programRepository          repositorio de programas
     * @param studentRepository          repositorio de estudiantes (para contar registros)
     * @param accessScope                reglas de alcance
     * @param clock                      reloj de la aplicación
     */
    public RegistrationCodeServiceImpl(RegistrationCodeRepository registrationCodeRepository,
                                       TeacherRepository teacherRepository, ProgramRepository programRepository,
                                       StudentRepository studentRepository, AccessScope accessScope, Clock clock) {
        this.registrationCodeRepository = registrationCodeRepository;
        this.teacherRepository = teacherRepository;
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public RegistrationCodeResponse create(AuthenticatedUser actor, NewRegistrationCodeRequest request) {
        User creator = accessScope.loadActor(actor);
        Teacher owner = resolveOwner(creator, request.teacherId());
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Programa", request.programId()));
        if (!Objects.equals(program.getFaculty().getId(), owner.getFaculty().getId())) {
            throw new BusinessRuleException("El programa no pertenece a la facultad del docente.");
        }
        if (request.expiresOn().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("La fecha de vencimiento no puede estar en el pasado.");
        }
        RegistrationCode code = registrationCodeRepository.save(new RegistrationCode(newUniqueCode(), owner,
                program, request.expiresOn(), LocalDateTime.now(clock)));
        return RegistrationCodeMapper.toResponse(code, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationCodeResponse> list(AuthenticatedUser actor) {
        User viewer = accessScope.loadActor(actor);
        List<RegistrationCode> codes;
        if (viewer instanceof Teacher teacher) {
            codes = registrationCodeRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId());
        } else if (viewer instanceof Admin admin) {
            codes = registrationCodeRepository.findByTeacherFacultyIdOrderByCreatedAtDesc(admin.getFaculty().getId());
        } else {
            codes = registrationCodeRepository.findAllByOrderByCreatedAtDesc();
        }
        return codes.stream().map(this::toResponse).toList();
    }

    @Override
    public RegistrationCodeResponse update(AuthenticatedUser actor, Long id, UpdateRegistrationCodeRequest request) {
        RegistrationCode code = registrationCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de registro", id));
        User viewer = accessScope.loadActor(actor);
        boolean isOwner = viewer instanceof Teacher && Objects.equals(viewer.getId(), code.getTeacher().getId());
        if (!isOwner) {
            if (viewer instanceof Teacher) {
                throw new ForbiddenAccessException("Solo puede gestionar sus propios códigos de registro.");
            }
            accessScope.requireFaculty(viewer, code.getTeacher().getFaculty().getId());
        }
        if (request.active()) {
            if (!code.isActive()) {
                throw new BusinessRuleException("Un código desactivado no se puede reactivar; genere uno nuevo.");
            }
        } else {
            code.deactivate();
        }
        return toResponse(code);
    }

    /**
     * Dueño del código: el docente que lo pide o, si lo pide un admin o el
     * super_admin, el docente indicado (dentro de su alcance).
     */
    private Teacher resolveOwner(User creator, Long teacherId) {
        if (creator instanceof Teacher teacher) {
            if (teacherId != null && !teacherId.equals(teacher.getId())) {
                throw new ForbiddenAccessException("Solo puede generar códigos a su nombre.");
            }
            return teacher;
        }
        if (teacherId == null) {
            throw new BusinessRuleException("Indique el docente dueño del código.");
        }
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Docente", teacherId));
        accessScope.requireFaculty(creator, teacher.getFaculty().getId());
        return teacher;
    }

    private String newUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (!registrationCodeRepository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("No se pudo generar un código de registro único");
    }

    private RegistrationCodeResponse toResponse(RegistrationCode code) {
        return RegistrationCodeMapper.toResponse(code, studentRepository.countByRegistrationCodeId(code.getId()));
    }
}
