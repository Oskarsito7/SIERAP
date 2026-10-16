package co.edu.unicesar.sierap.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse.Adjustment;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse.CriterionForm;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse.MemberForm;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse.OutcomeForm;
import co.edu.unicesar.sierap.dto.response.EvaluationFormResponse.SubmissionRef;
import co.edu.unicesar.sierap.dto.response.EvaluationSummaryResponse;
import co.edu.unicesar.sierap.dto.response.EvaluationSummaryResponse.EvaluatorRef;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.entity.CriterionGrade;
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
import co.edu.unicesar.sierap.entity.User;
import org.hibernate.Hibernate;

/**
 * Convierte evaluaciones a sus DTO ({@code docs/07}, "Evaluation"). Recorre relaciones
 * LAZY (momento, RAP, criterios, niveles e integrantes), así que se llama dentro de
 * una transacción. Las notas salen con dos decimales (ADR-013).
 */
public final class EvaluationMapper {

    /** Nombre del tipo de evaluador del docente del grupo. */
    public static final String TEACHER_KIND = "Docente";

    private static final Comparator<User> BY_NAME =
            Comparator.comparing(User::getLastNames).thenComparing(User::getFirstNames);

    private EvaluationMapper() {
    }

    /**
     * Evaluación propia de un grupo, o una vacía si todavía no se empezó.
     *
     * @param group      grupo evaluado
     * @param evaluation evaluación del usuario; {@code null} si no la ha empezado
     * @param canEdit    si hoy está abierto el periodo de calificación del grupo
     * @return el formulario de la evaluación
     */
    public static EvaluationFormResponse toForm(ProjectGroup group, Evaluation evaluation, boolean canEdit) {
        Submission toGrade = group.getSubmissionToGrade();
        List<OutcomeForm> outcomes = group.getMoment().getLearningOutcomes().stream()
                .map(outcome -> toOutcome(outcome, evaluation))
                .toList();
        List<MemberForm> members = group.getMembers().stream()
                .sorted(BY_NAME)
                .map(member -> toMember(member, evaluation))
                .toList();
        return new EvaluationFormResponse(
                evaluation == null ? null : evaluation.getId(),
                group.getId(),
                evaluation == null ? null : evaluation.getStatus(),
                evaluation == null ? null : evaluation.getEvaluatedAt(),
                evaluation == null ? null : evaluation.getComment(),
                canEdit,
                toGrade == null
                        ? null
                        : new SubmissionRef(toGrade.getId(), toGrade.getVersionNumber(), toGrade.isLate()),
                outcomes,
                members,
                score(evaluation == null ? BigDecimal.ZERO : evaluation.getTotalScore()),
                score(group.getMoment().getMaxScore()),
                evaluation != null && evaluation.isComplete());
    }

    /**
     * Evaluación en la lista de evaluaciones del grupo.
     *
     * @param evaluation evaluación
     * @return resumen con el evaluador y su tipo
     */
    public static EvaluationSummaryResponse toSummary(Evaluation evaluation) {
        User evaluator = (User) Hibernate.unproxy(evaluation.getEvaluator());
        return new EvaluationSummaryResponse(
                evaluation.getId(),
                new EvaluatorRef(evaluator.getId(), evaluator.getFullName(), kindOf(evaluator)),
                evaluation.getStatus(),
                evaluation.getEvaluatedAt(),
                score(evaluation.getTotalScore()),
                evaluation.getComment());
    }

    /**
     * Tipo de evaluador como lo ve el usuario: "Docente" o el nombre del tipo del
     * evaluador externo (p. ej. "Empresario").
     *
     * @param evaluator usuario evaluador, sin proxy de Hibernate
     * @return el tipo para mostrar
     */
    public static String kindOf(User evaluator) {
        return evaluator instanceof ExternalEvaluator external ? external.getEvaluatorType().getName() : TEACHER_KIND;
    }

    private static OutcomeForm toOutcome(LearningOutcome outcome, Evaluation evaluation) {
        return new OutcomeForm(outcome.getId(), outcome.getCode(), outcome.getDescription(), outcome.getMaxScore(),
                outcome.getCriteria().stream().map(criterion -> toCriterion(criterion, evaluation)).toList());
    }

    private static CriterionForm toCriterion(EvaluationCriterion criterion, Evaluation evaluation) {
        Long levelId = evaluation == null ? null : evaluation.getGrades().stream()
                .filter(grade -> grade.getCriterion().getId().equals(criterion.getId()))
                .findFirst()
                .map(CriterionGrade::getLevel)
                .map(PerformanceLevel::getId)
                .orElse(null);
        return new CriterionForm(criterion.getId(), criterion.getDescription(),
                criterion.getLevels().stream()
                        .map(level -> new LevelResponse(level.getId(), level.getName(), level.getDescription(),
                                level.getScore(), level.getDisplayOrder()))
                        .toList(),
                levelId);
    }

    private static MemberForm toMember(Student member, Evaluation evaluation) {
        if (evaluation == null) {
            return new MemberForm(member.getId(), member.getFullName(), List.of(), score(BigDecimal.ZERO));
        }
        List<Adjustment> adjustments = evaluation.getAdjustmentsFor(member).stream()
                .map(adjustment -> new Adjustment(adjustment.getCriterion().getId(), adjustment.getLevel().getId()))
                .toList();
        return new MemberForm(member.getId(), member.getFullName(), adjustments,
                score(evaluation.getTotalScoreFor(member)));
    }

    private static BigDecimal score(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
