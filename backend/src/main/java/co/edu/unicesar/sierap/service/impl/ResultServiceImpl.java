package co.edu.unicesar.sierap.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import co.edu.unicesar.sierap.dto.response.GroupResultsResponse;
import co.edu.unicesar.sierap.dto.response.GroupResultsResponse.MemberResultResponse;
import co.edu.unicesar.sierap.dto.response.GroupResultsResponse.OutcomeScoreResponse;
import co.edu.unicesar.sierap.dto.response.StudentResultResponse;
import co.edu.unicesar.sierap.dto.response.StudentResultResponse.EvaluatorCommentResponse;
import co.edu.unicesar.sierap.dto.response.StudentResultResponse.StudentOutcomeScoreResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.EvaluationMapper;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.ResultService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ResultService}.
 *
 * <p>Las notas las calcula el modelo ({@link ProjectGroup#getGroupGrade()} y
 * {@link ProjectGroup#getIndividualGrade(Student)}, fórmula de ADR-018); aquí se valida
 * quién las ve y cuándo, con el {@code Clock} inyectado:</p>
 * <ul>
 *   <li>El docente del grupo, el admin de su facultad y el super_admin ven los
 *       resultados del grupo en cualquier momento; mientras la ventana de calificación
 *       está abierta son una vista previa.</li>
 *   <li>El estudiante ve sus notas, su desempeño por RAP y los comentarios solo cuando
 *       cierra la ventana de calificación, y nunca los niveles que eligió cada evaluador.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ResultServiceImpl implements ResultService {

    private static final Comparator<User> BY_NAME =
            Comparator.comparing(User::getLastNames).thenComparing(User::getFirstNames);

    private final ProjectGroupRepository projectGroupRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el service con sus dependencias.
     *
     * @param projectGroupRepository grupos
     * @param accessScope            carga del usuario autenticado
     * @param clock                  reloj de la aplicación
     */
    public ResultServiceImpl(ProjectGroupRepository projectGroupRepository, AccessScope accessScope, Clock clock) {
        this.projectGroupRepository = projectGroupRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    public GroupResultsResponse groupResults(AuthenticatedUser actor, Long groupId) {
        User viewer = accessScope.loadActor(actor);
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo", groupId));
        if (!canSeeResults(viewer, group)) {
            throw new ForbiddenAccessException(
                    "Solo el docente del grupo, el admin de su facultad y el super_admin ven sus resultados.");
        }

        Moment moment = group.getMoment();
        List<OutcomeScoreResponse> groupOutcomes = moment.getLearningOutcomes().stream()
                .map(outcome -> outcomeScore(outcome, group.getGroupScoreFor(outcome).orElse(null)))
                .toList();
        List<MemberResultResponse> members = group.getMembers().stream()
                .sorted(BY_NAME)
                .map(student -> new MemberResultResponse(student.getId(), student.getFullName(),
                        group.getIndividualGrade(student).orElse(null),
                        moment.getLearningOutcomes().stream()
                                .map(outcome -> outcomeScore(outcome,
                                        group.getIndividualScoreFor(outcome, student).orElse(null)))
                                .toList()))
                .toList();
        List<Evaluation> submitted = group.getSubmittedEvaluations();

        return new GroupResultsResponse(group.getId(), isGradingClosed(moment), moment.getMaxScore(),
                group.getGroupGrade().orElse(null), groupOutcomes, members, submitted.size(),
                pendingEvaluations(group, submitted));
    }

    @Override
    public List<StudentResultResponse> myResults(AuthenticatedUser actor) {
        if (!(accessScope.loadActor(actor) instanceof Student student)) {
            throw new ForbiddenAccessException("Solo los estudiantes consultan sus notas.");
        }
        return projectGroupRepository.findByMembersIdOrderByMomentPeriodStartDateDescMomentNumberDesc(student.getId())
                .stream()
                .map(group -> studentResult(group, student))
                .toList();
    }

    // ---------------------------------------------------------------- reglas comunes

    /** El docente que habilitó el grupo, el admin de su facultad y el super_admin. */
    private static boolean canSeeResults(User viewer, ProjectGroup group) {
        if (viewer instanceof SuperAdmin) {
            return true;
        }
        if (viewer instanceof Admin admin) {
            return Objects.equals(admin.getFaculty().getId(), group.getMoment().getProgram().getFaculty().getId());
        }
        return viewer instanceof Teacher teacher && group.isEnabledBy(teacher);
    }

    /** Las notas son definitivas cuando el momento está lanzado y ya cerró su ventana de calificación. */
    private boolean isGradingClosed(Moment moment) {
        LocalDate gradingEnd = moment.getGradingEnd();
        return moment.getStatus() == MomentStatus.LAUNCHED && gradingEnd != null
                && LocalDate.now(clock).isAfter(gradingEnd);
    }

    /** El docente y los evaluadores externos asignados que todavía no envían su evaluación. */
    private static int pendingEvaluations(ProjectGroup group, List<Evaluation> submitted) {
        Set<Long> submittedBy = submitted.stream()
                .map(evaluation -> evaluation.getEvaluator().getId())
                .collect(Collectors.toSet());
        int pending = submittedBy.contains(group.getTeacher().getId()) ? 0 : 1;
        pending += (int) group.getEvaluators().stream()
                .filter(evaluator -> !submittedBy.contains(evaluator.getId()))
                .count();
        return pending;
    }

    private StudentResultResponse studentResult(ProjectGroup group, Student student) {
        Moment moment = group.getMoment();
        boolean available = isGradingClosed(moment);
        BigDecimal finalGrade = null;
        BigDecimal groupGrade = null;
        List<StudentOutcomeScoreResponse> outcomes = List.of();
        List<EvaluatorCommentResponse> comments = List.of();
        if (available) {
            finalGrade = group.getIndividualGrade(student).orElse(null);
            groupGrade = group.getGroupGrade().orElse(null);
            outcomes = moment.getLearningOutcomes().stream()
                    .map(outcome -> new StudentOutcomeScoreResponse(outcome.getCode(), outcome.getDescription(),
                            group.getIndividualScoreFor(outcome, student).orElse(null), outcome.getMaxScore()))
                    .toList();
            // Incluye los comentarios de los tipos sin peso: su nota no cuenta, pero el
            // comentario sí le sirve al estudiante (decisión del 2026-10-16).
            comments = comments(group.getSubmittedEvaluations());
        }
        return new StudentResultResponse(moment.getId(), moment.getName(), moment.getPeriod().getCode(),
                group.getId(), group.getName(), group.getProjectTitle(), available, finalGrade, groupGrade,
                moment.getMaxScore(), outcomes, comments);
    }

    /** Comentarios de las evaluaciones enviadas: primero el del docente, luego por tipo de evaluador. */
    private static List<EvaluatorCommentResponse> comments(List<Evaluation> submitted) {
        return submitted.stream()
                .filter(evaluation -> evaluation.getComment() != null && !evaluation.getComment().isBlank())
                .map(evaluation -> new EvaluatorCommentResponse(
                        EvaluationMapper.kindOf((User) Hibernate.unproxy(evaluation.getEvaluator())),
                        evaluation.getComment()))
                .sorted(Comparator.comparing((EvaluatorCommentResponse comment) ->
                                !EvaluationMapper.TEACHER_KIND.equals(comment.evaluatorKind()))
                        .thenComparing(EvaluatorCommentResponse::evaluatorKind))
                .toList();
    }

    private static OutcomeScoreResponse outcomeScore(LearningOutcome outcome, BigDecimal score) {
        return new OutcomeScoreResponse(outcome.getId(), outcome.getCode(), score, outcome.getMaxScore());
    }
}
