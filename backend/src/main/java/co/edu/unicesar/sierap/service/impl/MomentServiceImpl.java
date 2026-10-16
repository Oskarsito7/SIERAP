package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.MomentMapper;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.MomentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link MomentService}.
 *
 * <p>Es de solo lectura y transaccional: los DTO recorren relaciones LAZY (programa,
 * periodo, RAP, criterios y niveles) y la aplicación no mantiene la sesión abierta
 * en la vista ({@code open-in-view: false}).</p>
 */
@Service
@Transactional(readOnly = true)
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param momentRepository         repositorio de momentos
     * @param academicPeriodRepository repositorio de periodos académicos
     * @param projectGroupRepository   repositorio de grupos de proyecto
     * @param accessScope              reglas de alcance
     * @param clock                    reloj de la aplicación
     */
    public MomentServiceImpl(MomentRepository momentRepository, AcademicPeriodRepository academicPeriodRepository,
                             ProjectGroupRepository projectGroupRepository,
                             AccessScope accessScope, Clock clock) {
        this.momentRepository = momentRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public List<MomentSummaryResponse> list(AuthenticatedUser actor, Long periodId) {
        LocalDate today = LocalDate.now(clock);
        Optional<AcademicPeriod> period = periodId == null
                ? academicPeriodRepository
                        .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                : Optional.of(academicPeriodRepository.findById(periodId)
                        .orElseThrow(() -> new ResourceNotFoundException("Periodo académico", periodId)));
        if (period.isEmpty()) {
            return List.of();
        }
        Long id = period.get().getId();
        User viewer = accessScope.loadActor(actor);
        List<Moment> moments;
        if (viewer instanceof SuperAdmin) {
            moments = momentRepository.findByPeriodIdOrderByProgramNameAscNumberAsc(id);
        } else if (viewer instanceof Student student) {
            moments = momentRepository.findForStudent(id, student.getProgram().getId(),
                    student.getCurrentSemester(), student.getId());
        } else {
            moments = momentRepository.findByPeriodIdAndProgramFacultyIdOrderByProgramNameAscNumberAsc(id,
                    requireFaculty(viewer));
        }
        return moments.stream().map(moment -> MomentMapper.toSummary(moment, today)).toList();
    }

    @Override
    public MomentDetailResponse get(AuthenticatedUser actor, Long id) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", id));
        if (!canView(accessScope.loadActor(actor), moment)) {
            throw new ForbiddenAccessException("No tiene acceso a este momento.");
        }
        return MomentMapper.toDetail(moment, LocalDate.now(clock));
    }

    /**
     * Indica si el usuario puede ver el momento: el super_admin siempre; el admin y
     * el docente si el programa es de su facultad; el estudiante si es de su programa
     * y su semestre actual está en el rango del momento, o si ya tiene un grupo
     * en ese momento (un docente puede haberlo agregado fuera de rango).
     */
    private boolean canView(User viewer, Moment moment) {
        if (viewer instanceof SuperAdmin) {
            return true;
        }
        if (viewer instanceof Student student) {
            int semester = student.getCurrentSemester();
            boolean inSemesterRange = Objects.equals(student.getProgram().getId(), moment.getProgram().getId())
                    && semester >= moment.getSemesterFrom() && semester <= moment.getSemesterTo();
            if (inSemesterRange) {
                return true;
            }
            return projectGroupRepository.existsByMomentIdAndMembersId(moment.getId(), student.getId());
        }
        return Objects.equals(requireFaculty(viewer), moment.getProgram().getFaculty().getId());
    }

    /**
     * Facultad del admin o del docente. Los demás roles no llegan aquí (el controller
     * no los admite), pero si llegaran no ven nada.
     */
    private Long requireFaculty(User viewer) {
        Long faculty = accessScope.facultyOf(viewer);
        if (faculty == null) {
            throw new ForbiddenAccessException("No tiene acceso a los momentos.");
        }
        return faculty;
    }
}
