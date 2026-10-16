package co.edu.unicesar.sierap.service.impl;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewFacultyRequest;
import co.edu.unicesar.sierap.dto.request.UpdateFacultyRequest;
import co.edu.unicesar.sierap.dto.response.FacultyResponse;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AdminRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.TeacherRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.FacultyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link FacultyService}.
 */
@Service
@Transactional(readOnly = true)
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository facultyRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final AccessScope accessScope;

    /**
     * Crea el servicio.
     *
     * @param facultyRepository      repositorio de facultades
     * @param projectGroupRepository repositorio de grupos
     * @param studentRepository      repositorio de estudiantes
     * @param adminRepository        repositorio de administradores
     * @param teacherRepository      repositorio de docentes
     * @param accessScope            reglas de alcance por rol
     */
    public FacultyServiceImpl(FacultyRepository facultyRepository,
                              ProjectGroupRepository projectGroupRepository,
                              StudentRepository studentRepository,
                              AdminRepository adminRepository,
                              TeacherRepository teacherRepository,
                              AccessScope accessScope) {
        this.facultyRepository = facultyRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.accessScope = accessScope;
    }

    @Override
    public List<FacultyResponse> listForUser(AuthenticatedUser actor) {
        User user = accessScope.loadActor(actor);
        if (user instanceof SuperAdmin) {
            return facultyRepository.findAllByOrderByNameAsc().stream()
                    .map(FacultyServiceImpl::toResponse)
                    .toList();
        }
        Long facultyId = accessScope.facultyOf(user);
        if (facultyId == null) {
            throw new ForbiddenAccessException("Su rol no permite consultar facultades.");
        }
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Facultad", facultyId));
        return List.of(toResponse(faculty));
    }

    @Override
    @Transactional
    public FacultyResponse create(NewFacultyRequest request) {
        String code = request.code().trim();
        if (facultyRepository.existsByCode(code)) {
            throw new DataIntegrityViolationException(
                    "Ya existe una facultad con el código '" + code + "'.");
        }
        Faculty faculty = new Faculty(code, request.name().trim());
        Faculty saved = facultyRepository.save(faculty);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FacultyResponse update(Long id, UpdateFacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facultad", id));
        String code = request.code().trim();
        if (facultyRepository.existsByCodeAndIdNot(code, id)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otra facultad con el código '" + code + "'.");
        }
        faculty.setCode(code);
        faculty.setName(request.name().trim());
        return toResponse(faculty);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facultad", id));
        if (projectGroupRepository.existsByMomentProgramFacultyId(id)
                || studentRepository.existsByProgramFacultyId(id)
                || adminRepository.existsByFacultyId(id)
                || teacherRepository.existsByFacultyId(id)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar la facultad porque tiene grupos, estudiantes o cuentas adscritas.");
        }
        facultyRepository.delete(faculty);
    }

    private static FacultyResponse toResponse(Faculty faculty) {
        return new FacultyResponse(
                faculty.getId(),
                faculty.getCode(),
                faculty.getName(),
                faculty.getPrograms().size());
    }
}
