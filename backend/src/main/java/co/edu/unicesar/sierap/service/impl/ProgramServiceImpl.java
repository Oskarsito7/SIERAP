package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewProgramRequest;
import co.edu.unicesar.sierap.dto.request.UpdateProgramRequest;
import co.edu.unicesar.sierap.dto.response.ProgramResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ProgramService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ProgramService}.
 */
@Service
@Transactional(readOnly = true)
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final FacultyRepository facultyRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final MomentRepository momentRepository;
    private final StudentRepository studentRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final ExternalEvaluatorRepository externalEvaluatorRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param programRepository           repositorio de programas
     * @param facultyRepository           repositorio de facultades
     * @param academicPeriodRepository    repositorio de periodos académicos
     * @param momentRepository            repositorio de momentos
     * @param studentRepository           repositorio de estudiantes
     * @param projectGroupRepository      repositorio de grupos
     * @param externalEvaluatorRepository repositorio de evaluadores externos
     * @param accessScope                 reglas de alcance por rol
     * @param clock                       reloj del sistema
     */
    public ProgramServiceImpl(ProgramRepository programRepository,
                              FacultyRepository facultyRepository,
                              AcademicPeriodRepository academicPeriodRepository,
                              MomentRepository momentRepository,
                              StudentRepository studentRepository,
                              ProjectGroupRepository projectGroupRepository,
                              ExternalEvaluatorRepository externalEvaluatorRepository,
                              AccessScope accessScope,
                              Clock clock) {
        this.programRepository = programRepository;
        this.facultyRepository = facultyRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.momentRepository = momentRepository;
        this.studentRepository = studentRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.externalEvaluatorRepository = externalEvaluatorRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public List<ProgramResponse> listForUser(AuthenticatedUser actor, Long facultyId) {
        User user = accessScope.loadActor(actor);
        List<Program> programs;
        if (user instanceof SuperAdmin) {
            programs = facultyId != null
                    ? programRepository.findByFacultyIdOrderByNameAsc(facultyId)
                    : programRepository.findAllByOrderByNameAsc();
        } else {
            Long ownFacultyId = accessScope.facultyOf(user);
            if (ownFacultyId == null) {
                throw new ForbiddenAccessException("Su rol no permite consultar programas.");
            }
            if (facultyId != null) {
                accessScope.requireFaculty(user, facultyId);
            }
            programs = programRepository.findByFacultyIdOrderByNameAsc(ownFacultyId);
        }

        Long currentPeriodId = currentPeriodId().orElse(null);
        return programs.stream()
                .map(program -> toResponse(program, currentPeriodId))
                .toList();
    }

    @Override
    @Transactional
    public ProgramResponse create(NewProgramRequest request) {
        Faculty faculty = facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Facultad", request.facultyId()));
        String code = request.code().trim();
        if (programRepository.existsByCode(code)) {
            throw new DataIntegrityViolationException(
                    "Ya existe un programa con el código '" + code + "'.");
        }
        Program program = new Program(code, request.name().trim());
        faculty.addProgram(program);
        Program saved = programRepository.save(program);
        return toResponse(saved, currentPeriodId().orElse(null));
    }

    @Override
    @Transactional
    public ProgramResponse update(Long id, UpdateProgramRequest request) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programa", id));
        String code = request.code().trim();
        if (programRepository.existsByCodeAndIdNot(code, id)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro programa con el código '" + code + "'.");
        }
        program.setCode(code);
        program.setName(request.name().trim());
        return toResponse(program, currentPeriodId().orElse(null));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programa", id));
        if (projectGroupRepository.existsByMomentProgramId(id)
                || studentRepository.existsByProgramId(id)
                || externalEvaluatorRepository.existsByGraduationProgramId(id)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar el programa porque tiene grupos, estudiantes o evaluadores asociados.");
        }
        program.getFaculty().removeProgram(program);
        programRepository.delete(program);
    }

    private Optional<Long> currentPeriodId() {
        LocalDate today = LocalDate.now(clock);
        return academicPeriodRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .map(AcademicPeriod::getId);
    }

    private ProgramResponse toResponse(Program program, Long currentPeriodId) {
        int currentPeriodMomentCount = currentPeriodId != null
                ? momentRepository.countByProgramIdAndPeriodId(program.getId(), currentPeriodId)
                : 0;
        int studentCount = studentRepository.countByProgramId(program.getId());
        return new ProgramResponse(
                program.getId(),
                program.getCode(),
                program.getName(),
                program.getFaculty().getId(),
                program.getFaculty().getName(),
                currentPeriodMomentCount,
                studentCount);
    }
}
