package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unicesar.sierap.dto.request.SaveEvaluationRequest;
import co.edu.unicesar.sierap.dto.request.SaveEvaluationRequest.AdjustmentChoice;
import co.edu.unicesar.sierap.dto.request.SaveEvaluationRequest.LevelChoice;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse;
import co.edu.unicesar.sierap.dto.response.EvaluationSummaryResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.Evaluator;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.IndividualGrade;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.EvaluationMapper;
import co.edu.unicesar.sierap.repository.EvaluationRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.EvaluationService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link EvaluationService}.
 *
 * <p>Quién califica lo decide cada evaluador con su propia regla
 * ({@link Evaluator#canEvaluate(ProjectGroup)}, polimorfismo); cuándo, el periodo de
 * calificación del grupo con el {@code Clock} inyectado. Las reglas de los niveles y
 * los ajustes viven en {@link Evaluation}; aquí se valida lo que viene de la API
 * (criterios del momento, niveles del criterio, integrantes del grupo) y se responde
 * 403, 404 o 422 con un mensaje en español.</p>
 */
@Service
@Transactional
public class EvaluationServiceImpl implements EvaluationService {

    private final ProjectGroupRepository projectGroupRepository;
    private final EvaluationRepository evaluationRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el service con sus dependencias.
     *
     * @param projectGroupRepository grupos
     * @param evaluationRepository   evaluaciones
     * @param accessScope            carga del usuario autenticado
     * @param clock                  reloj de la aplicación
     */
    public EvaluationServiceImpl(ProjectGroupRepository projectGroupRepository,
                                 EvaluationRepository evaluationRepository, AccessScope accessScope, Clock clock) {
        this.projectGroupRepository = projectGroupRepository;
        this.evaluationRepository = evaluationRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationFormResponse form(AuthenticatedUser actor, Long groupId) {
        User evaluator = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(groupId);
        requireEvaluator(evaluator, group);
        Evaluation evaluation = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), evaluator.getId())
                .orElse(null);
        return EvaluationMapper.toForm(group, evaluation, gradingClosedReason(group, today()) == null);
    }

    @Override
    public EvaluationFormResponse save(AuthenticatedUser actor, Long groupId, SaveEvaluationRequest request) {
        User evaluator = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(groupId);
        requireEvaluator(evaluator, group);
        requireGradingOpen(group);

        // Se valida todo antes de cambiar nada, para no dejar la evaluación a medias.
        Map<Long, EvaluationCriterion> criteria = criteriaOf(group.getMoment());
        List<PerformanceLevel> levels = request.levels() == null ? List.of()
                : request.levels().stream().map(choice -> resolveLevel(criteria, choice.criterionId(),
                        choice.levelId())).toList();
        Map<String, ResolvedAdjustment> adjustments = request.adjustments() == null ? null
                : resolveAdjustments(group, criteria, request.adjustments());

        LocalDateTime now = now();
        Evaluation evaluation = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), evaluator.getId())
                .orElseGet(() -> {
                    Evaluation created = new Evaluation(evaluator, now);
                    group.addEvaluation(created);
                    return evaluationRepository.save(created);
                });
        levels.forEach(evaluation::grade);
        if (request.comment() != null) {
            evaluation.setComment(request.comment().isBlank() ? null : request.comment().strip());
        }
        if (adjustments != null) {
            replaceAdjustments(evaluation, adjustments);
        }
        if (evaluation.isSubmitted() && !evaluation.isComplete()) {
            throw new BusinessRuleException("La evaluación ya fue enviada: todos los criterios deben tener nivel.");
        }
        evaluation.setEvaluatedAt(now);
        return EvaluationMapper.toForm(group, evaluation, true);
    }

    @Override
    public EvaluationFormResponse submit(AuthenticatedUser actor, Long groupId) {
        User evaluator = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(groupId);
        requireEvaluator(evaluator, group);
        requireGradingOpen(group);
        Evaluation evaluation = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), evaluator.getId())
                .orElse(null);
        long missing = criteriaOf(group.getMoment()).size() - (evaluation == null ? 0 : evaluation.getGrades().size());
        if (evaluation == null || !evaluation.isComplete()) {
            throw new BusinessRuleException(missing == 1
                    ? "Falta calificar 1 criterio."
                    : "Falta calificar " + missing + " criterios.");
        }
        evaluation.submit();
        evaluation.setEvaluatedAt(now());
        return EvaluationMapper.toForm(group, evaluation, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationSummaryResponse> list(AuthenticatedUser actor, Long groupId) {
        User viewer = accessScope.loadActor(actor);
        ProjectGroup group = findGroup(groupId);
        List<Evaluation> evaluations;
        if (viewer instanceof ExternalEvaluator external && external.canEvaluate(group)) {
            evaluations = evaluationRepository.findByProjectGroupIdAndEvaluatorId(group.getId(), viewer.getId())
                    .map(List::of)
                    .orElse(List.of());
        } else if (canSeeAll(viewer, group)) {
            evaluations = evaluationRepository.findByProjectGroupId(group.getId());
        } else {
            throw new ForbiddenAccessException("No tiene acceso a las evaluaciones de este grupo.");
        }
        return evaluations.stream()
                .map(EvaluationMapper::toSummary)
                .sorted(Comparator.comparing((EvaluationSummaryResponse summary) ->
                                !EvaluationMapper.TEACHER_KIND.equals(summary.evaluator().kind()))
                        .thenComparing(summary -> summary.evaluator().fullName()))
                .toList();
    }

    // ---------------------------------------------------------------- reglas comunes

    /** Solo califica el docente del grupo o un evaluador externo asignado (polimorfismo). */
    private static void requireEvaluator(User user, ProjectGroup group) {
        if (!(user instanceof Evaluator evaluator) || !evaluator.canEvaluate(group)) {
            throw new ForbiddenAccessException(
                    "Solo el docente del grupo y sus evaluadores asignados pueden calificarlo.");
        }
    }

    /** El docente del grupo, el admin de su facultad y el super_admin ven todas las evaluaciones. */
    private static boolean canSeeAll(User viewer, ProjectGroup group) {
        if (viewer instanceof SuperAdmin) {
            return true;
        }
        if (viewer instanceof Admin admin) {
            return Objects.equals(admin.getFaculty().getId(), group.getMoment().getProgram().getFaculty().getId());
        }
        return viewer instanceof Teacher teacher && group.isEnabledBy(teacher);
    }

    private void requireGradingOpen(ProjectGroup group) {
        String reason = gradingClosedReason(group, today());
        if (reason != null) {
            throw new BusinessRuleException(reason);
        }
    }

    /**
     * Por qué hoy no se puede calificar el grupo, o {@code null} si se puede: el
     * momento debe estar lanzado y hoy debe estar entre la fecha de evaluación del
     * grupo (o el inicio de la ventana, si el docente no la fijó) y el fin de la
     * ventana de calificación (docs/07).
     */
    private static String gradingClosedReason(ProjectGroup group, LocalDate today) {
        Moment moment = group.getMoment();
        if (moment.getStatus() != MomentStatus.LAUNCHED) {
            return "El momento todavía no se ha lanzado: aún no se puede calificar.";
        }
        LocalDate end = moment.getGradingEnd();
        LocalDate start = group.getEvaluationDate() != null ? group.getEvaluationDate() : moment.getGradingStart();
        if (start == null || end == null) {
            return "El momento no tiene definida la ventana de calificación.";
        }
        if (today.isBefore(start)) {
            return group.getEvaluationDate() != null
                    ? "La evaluación de este grupo es el " + start + ": antes no se puede calificar."
                    : "La ventana de calificación abre el " + start + ".";
        }
        if (today.isAfter(end)) {
            return "La ventana de calificación cerró el " + end + ".";
        }
        return null;
    }

    /** Criterios de todos los RAP del momento, por id. */
    private static Map<Long, EvaluationCriterion> criteriaOf(Moment moment) {
        return moment.getLearningOutcomes().stream()
                .flatMap(outcome -> outcome.getCriteria().stream())
                .collect(Collectors.toMap(EvaluationCriterion::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
    }

    private static PerformanceLevel resolveLevel(Map<Long, EvaluationCriterion> criteria, Long criterionId,
                                                 Long levelId) {
        EvaluationCriterion criterion = criteria.get(criterionId);
        if (criterion == null) {
            throw new BusinessRuleException("El criterio " + criterionId + " no es de este momento.");
        }
        return criterion.getLevels().stream()
                .filter(level -> level.getId().equals(levelId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El nivel " + levelId + " no es del criterio \""
                        + criterion.getDescription() + "\"."));
    }

    /** Valida los ajustes enviados; si un estudiante y criterio se repiten, cuenta el último. */
    private static Map<String, ResolvedAdjustment> resolveAdjustments(ProjectGroup group,
                                                                      Map<Long, EvaluationCriterion> criteria,
                                                                      List<AdjustmentChoice> choices) {
        Map<Long, Student> members = group.getMembers().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        Map<String, ResolvedAdjustment> resolved = new LinkedHashMap<>();
        for (AdjustmentChoice choice : choices) {
            Student student = members.get(choice.studentId());
            if (student == null) {
                throw new BusinessRuleException("El estudiante " + choice.studentId()
                        + " no es integrante del grupo.");
            }
            PerformanceLevel level = resolveLevel(criteria, choice.criterionId(), choice.levelId());
            resolved.put(key(student.getId(), choice.criterionId()), new ResolvedAdjustment(student, level));
        }
        return resolved;
    }

    /**
     * Reemplaza los ajustes: quita los que ya no vienen y aplica los enviados. Los que
     * se repiten se cambian en la misma fila (borrar y volver a insertar violaría la
     * restricción única, porque Hibernate ejecuta los INSERT antes que los DELETE).
     */
    private static void replaceAdjustments(Evaluation evaluation, Map<String, ResolvedAdjustment> adjustments) {
        for (IndividualGrade current : List.copyOf(evaluation.getIndividualGrades())) {
            Student student = (Student) Hibernate.unproxy(current.getStudent());
            if (!adjustments.containsKey(key(student.getId(), current.getCriterion().getId()))) {
                evaluation.removeAdjustment(student, current.getCriterion());
            }
        }
        adjustments.values().forEach(adjustment -> evaluation.adjust(adjustment.student(), adjustment.level()));
    }

    private static String key(Long studentId, Long criterionId) {
        return studentId + ":" + criterionId;
    }

    private ProjectGroup findGroup(Long id) {
        return projectGroupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grupo", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /** Ajuste ya validado: integrante del grupo y nivel de un criterio del momento. */
    private record ResolvedAdjustment(Student student, PerformanceLevel level) {
    }
}
