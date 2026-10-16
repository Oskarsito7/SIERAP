package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.MomentMapper;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.MomentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link MomentService}.
 *
 * <p>Es transaccional: los DTO recorren relaciones LAZY (programa, periodo, pesos, RAP,
 * criterios y niveles) y la aplicación no mantiene la sesión abierta en la vista
 * ({@code open-in-view: false}).</p>
 */
@Service
@Transactional(readOnly = true)
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final ProgramRepository programRepository;
    private final EvaluatorTypeRepository evaluatorTypeRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param momentRepository         repositorio de momentos
     * @param academicPeriodRepository repositorio de periodos académicos
     * @param programRepository        repositorio de programas académicos
     * @param evaluatorTypeRepository  repositorio de tipos de evaluador
     * @param projectGroupRepository   repositorio de grupos de proyecto
     * @param accessScope              reglas de alcance
     * @param clock                    reloj de la aplicación
     */
    public MomentServiceImpl(MomentRepository momentRepository,
                             AcademicPeriodRepository academicPeriodRepository,
                             ProgramRepository programRepository,
                             EvaluatorTypeRepository evaluatorTypeRepository,
                             ProjectGroupRepository projectGroupRepository,
                             AccessScope accessScope,
                             Clock clock) {
        this.momentRepository = momentRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.programRepository = programRepository;
        this.evaluatorTypeRepository = evaluatorTypeRepository;
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

        List<Long> momentIds = moments.stream().map(Moment::getId).toList();
        Map<Long, Integer> groupCounts = new HashMap<>();
        if (!momentIds.isEmpty()) {
            for (Object[] row : projectGroupRepository.countByMomentIdIn(momentIds)) {
                groupCounts.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }

        return moments.stream()
                .map(moment -> MomentMapper.toSummary(moment, today, groupCounts.getOrDefault(moment.getId(), 0)))
                .toList();
    }

    @Override
    public MomentDetailResponse get(AuthenticatedUser actor, Long id) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", id));
        if (!canView(accessScope.loadActor(actor), moment)) {
            throw new ForbiddenAccessException("No tiene acceso a este momento.");
        }
        int groupCount = projectGroupRepository.countByMomentId(id);
        return MomentMapper.toDetail(moment, LocalDate.now(clock), groupCount);
    }

    @Override
    @Transactional
    public MomentDetailResponse create(AuthenticatedUser actor, NewMomentRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Programa", request.programId()));
        accessScope.requireFaculty(accessScope.loadActor(actor), program.getFaculty().getId());

        AcademicPeriod period = academicPeriodRepository.findById(request.periodId())
                .orElseThrow(() -> new ResourceNotFoundException("Periodo académico", request.periodId()));

        if (momentRepository.existsByProgramIdAndPeriodIdAndNumber(program.getId(), period.getId(), request.number())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un momento con el número " + request.number() + " para este programa y periodo.");
        }

        validateRanges(request.semesterFrom(), request.semesterTo(), request.minMembers(), request.maxMembers());
        validateWindows(request.groupFormationStart(), request.groupFormationEnd(),
                request.submissionStart(), request.submissionEnd(),
                request.gradingStart(), request.gradingEnd(),
                period.getUpcEvaluationDeadline());

        Moment moment = new Moment(period, request.number(), request.name(), request.subject(),
                request.semesterFrom(), request.semesterTo(), request.requirementLevel(),
                request.minMembers(), request.maxMembers());
        program.addMoment(moment);

        moment.setGroupFormationStart(request.groupFormationStart());
        moment.setGroupFormationEnd(request.groupFormationEnd());
        moment.setSubmissionStart(request.submissionStart());
        moment.setSubmissionEnd(request.submissionEnd());
        moment.setGradingStart(request.gradingStart());
        moment.setGradingEnd(request.gradingEnd());

        Moment saved = momentRepository.save(moment);
        return MomentMapper.toDetail(saved, LocalDate.now(clock), 0);
    }

    @Override
    @Transactional
    public MomentDetailResponse update(AuthenticatedUser actor, Long id, UpdateMomentRequest request) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", id));
        accessScope.requireFaculty(accessScope.loadActor(actor), moment.getProgram().getFaculty().getId());

        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento ya fue lanzado: su configuración no se puede modificar.");
        }

        AcademicPeriod targetPeriod = moment.getPeriod();
        if (request.periodId() != null && !request.periodId().equals(targetPeriod.getId())) {
            targetPeriod = academicPeriodRepository.findById(request.periodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Periodo académico", request.periodId()));
        }

        int targetNumber = request.number() != null ? request.number() : moment.getNumber();
        if (momentRepository.existsByProgramIdAndPeriodIdAndNumberAndIdNot(
                moment.getProgram().getId(), targetPeriod.getId(), targetNumber, moment.getId())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un momento con el número " + targetNumber + " para este programa y periodo.");
        }

        int targetSemesterFrom = request.semesterFrom() != null ? request.semesterFrom() : moment.getSemesterFrom();
        int targetSemesterTo = request.semesterTo() != null ? request.semesterTo() : moment.getSemesterTo();
        int targetMinMembers = request.minMembers() != null ? request.minMembers() : moment.getMinMembers();
        int targetMaxMembers = request.maxMembers() != null ? request.maxMembers() : moment.getMaxMembers();
        validateRanges(targetSemesterFrom, targetSemesterTo, targetMinMembers, targetMaxMembers);

        LocalDate targetGfStart = request.updateGroupFormationWindow() ? request.groupFormationStart() : moment.getGroupFormationStart();
        LocalDate targetGfEnd = request.updateGroupFormationWindow() ? request.groupFormationEnd() : moment.getGroupFormationEnd();
        LocalDate targetSubStart = request.updateSubmissionWindow() ? request.submissionStart() : moment.getSubmissionStart();
        LocalDate targetSubEnd = request.updateSubmissionWindow() ? request.submissionEnd() : moment.getSubmissionEnd();
        LocalDate targetGradStart = request.updateGradingWindow() ? request.gradingStart() : moment.getGradingStart();
        LocalDate targetGradEnd = request.updateGradingWindow() ? request.gradingEnd() : moment.getGradingEnd();

        validateWindows(targetGfStart, targetGfEnd, targetSubStart, targetSubEnd, targetGradStart, targetGradEnd,
                targetPeriod.getUpcEvaluationDeadline());

        moment.setPeriod(targetPeriod);
        moment.setNumber(targetNumber);
        if (request.name() != null) {
            moment.setName(request.name());
        }
        if (request.subject() != null) {
            moment.setSubject(request.subject());
        }
        moment.setSemesterFrom(targetSemesterFrom);
        moment.setSemesterTo(targetSemesterTo);
        if (request.requirementLevel() != null) {
            moment.setRequirementLevel(request.requirementLevel());
        }
        moment.setMinMembers(targetMinMembers);
        moment.setMaxMembers(targetMaxMembers);

        if (request.updateGroupFormationWindow()) {
            moment.setGroupFormationStart(targetGfStart);
            moment.setGroupFormationEnd(targetGfEnd);
        }
        if (request.updateSubmissionWindow()) {
            moment.setSubmissionStart(targetSubStart);
            moment.setSubmissionEnd(targetSubEnd);
        }
        if (request.updateGradingWindow()) {
            moment.setGradingStart(targetGradStart);
            moment.setGradingEnd(targetGradEnd);
        }

        int groupCount = projectGroupRepository.countByMomentId(moment.getId());
        return MomentMapper.toDetail(moment, LocalDate.now(clock), groupCount);
    }

    @Override
    @Transactional
    public void delete(AuthenticatedUser actor, Long id) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", id));
        accessScope.requireFaculty(accessScope.loadActor(actor), moment.getProgram().getFaculty().getId());

        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento ya fue lanzado: su configuración no se puede modificar.");
        }
        if (projectGroupRepository.existsByMomentId(id)) {
            throw new DataIntegrityViolationException("No se puede eliminar el momento porque tiene grupos asociados.");
        }
        momentRepository.delete(moment);
    }

    @Override
    @Transactional
    public MomentWeightsResponse updateWeights(AuthenticatedUser actor, Long id, MomentWeightsRequest request) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", id));
        accessScope.requireFaculty(accessScope.loadActor(actor), moment.getProgram().getFaculty().getId());

        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException("El momento ya fue lanzado: su configuración no se puede modificar.");
        }

        moment.setTeacherWeight(request.teacherWeight());

        Set<Long> seenTypes = new HashSet<>();
        if (request.evaluatorWeights() != null) {
            for (var item : request.evaluatorWeights()) {
                if (!seenTypes.add(item.evaluatorTypeId())) {
                    throw new BusinessRuleException("Un tipo de evaluador no puede aparecer más de una vez.");
                }
            }
        }

        moment.clearEvaluatorWeights();
        if (request.evaluatorWeights() != null) {
            for (var item : request.evaluatorWeights()) {
                EvaluatorType type = evaluatorTypeRepository.findById(item.evaluatorTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tipo de evaluador", item.evaluatorTypeId()));
                moment.setEvaluatorWeight(type, item.weight());
            }
        }

        List<MomentWeightsResponse.EvaluatorWeightItemResponse> items = moment.getEvaluatorWeights().stream()
                .map(w -> new MomentWeightsResponse.EvaluatorWeightItemResponse(
                        w.getEvaluatorType().getId(),
                        w.getEvaluatorType().getName(),
                        w.getWeight()))
                .toList();
        return new MomentWeightsResponse(moment.getTeacherWeight(), items, moment.getTotalWeight());
    }

    private void validateRanges(int semesterFrom, int semesterTo, int minMembers, int maxMembers) {
        if (semesterFrom > semesterTo) {
            throw new BusinessRuleException("El semestre inicial no puede ser mayor que el semestre final.");
        }
        if (minMembers > maxMembers) {
            throw new BusinessRuleException("El mínimo de integrantes no puede ser mayor que el máximo.");
        }
    }

    private void validateWindows(LocalDate gfStart, LocalDate gfEnd,
                                 LocalDate subStart, LocalDate subEnd,
                                 LocalDate gradStart, LocalDate gradEnd,
                                 LocalDate upcDeadline) {
        requireCompleteOrEmptyWindow("conformación de grupos", gfStart, gfEnd);
        requireCompleteOrEmptyWindow("entregas", subStart, subEnd);
        requireCompleteOrEmptyWindow("calificación", gradStart, gradEnd);

        if (gfEnd != null && subStart != null && !gfEnd.isBefore(subStart)) {
            throw new BusinessRuleException(
                    "La ventana de entregas debe empezar después de la de conformación de grupos.");
        }
        if (subEnd != null && gradStart != null && !subEnd.isBefore(gradStart)) {
            throw new BusinessRuleException(
                    "La ventana de calificación debe empezar después de la de entregas.");
        }
        if (gfEnd != null && gradStart != null && subStart == null && !gfEnd.isBefore(gradStart)) {
            throw new BusinessRuleException(
                    "La ventana de calificación debe empezar después de la de conformación de grupos.");
        }
        if (gradEnd != null && upcDeadline != null && gradEnd.isAfter(upcDeadline)) {
            throw new BusinessRuleException("La ventana de calificación debe terminar a más tardar el "
                    + upcDeadline + ", límite de evaluación de la UPC.");
        }
    }

    private void requireCompleteOrEmptyWindow(String window, LocalDate start, LocalDate end) {
        if ((start == null && end != null) || (start != null && end == null)) {
            throw new BusinessRuleException("La ventana de " + window + " debe tener fecha de inicio y de fin.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessRuleException("La ventana de " + window + " no puede terminar antes de empezar.");
        }
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
