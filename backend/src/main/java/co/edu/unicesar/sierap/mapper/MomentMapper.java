package co.edu.unicesar.sierap.mapper;

import java.time.LocalDate;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.EvaluatorWeightItemResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LearningOutcomeResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;

/**
 * Convierte momentos en sus DTO de salida.
 *
 * <p>Recorre relaciones LAZY (programa, periodo, pesos, RAP, criterios y niveles),
 * así que se debe llamar dentro de una transacción.</p>
 */
public final class MomentMapper {

    private MomentMapper() {
    }

    /**
     * Convierte un momento en su resumen sin conteo de grupos (por defecto 0).
     *
     * @param moment momento
     * @param today  fecha actual, obtenida del {@code Clock}
     * @return resumen del momento
     */
    public static MomentSummaryResponse toSummary(Moment moment, LocalDate today) {
        return toSummary(moment, today, 0);
    }

    /**
     * Convierte un momento en su resumen con el conteo de grupos.
     *
     * @param moment     momento
     * @param today      fecha actual, obtenida del {@code Clock}
     * @param groupCount cantidad de grupos habilitados
     * @return resumen del momento
     */
    public static MomentSummaryResponse toSummary(Moment moment, LocalDate today, int groupCount) {
        return new MomentSummaryResponse(moment.getId(), moment.getNumber(), moment.getName(),
                moment.getSubject(), moment.getProgram().getId(), moment.getProgram().getName(),
                moment.getPeriod().getId(), moment.getPeriod().getCode(), moment.getSemesterFrom(),
                moment.getSemesterTo(), moment.getRequirementLevel(), moment.getMinMembers(),
                moment.getMaxMembers(), groupCount, moment.getStatus(),
                moment.getGroupFormationStart(), moment.getGroupFormationEnd(), moment.getSubmissionStart(),
                moment.getSubmissionEnd(), moment.getGradingStart(), moment.getGradingEnd(),
                moment.isInGroupFormation(today), moment.isInSubmission(today), moment.isInGrading(today));
    }

    /**
     * Convierte un momento en su detalle sin conteo de grupos (por defecto 0).
     *
     * @param moment momento
     * @param today  fecha actual, obtenida del {@code Clock}
     * @return detalle del momento
     */
    public static MomentDetailResponse toDetail(Moment moment, LocalDate today) {
        return toDetail(moment, today, 0);
    }

    /**
     * Convierte un momento en su detalle, con la estructura de evaluación y conteo de grupos.
     *
     * @param moment     momento
     * @param today      fecha actual, obtenida del {@code Clock}
     * @param groupCount cantidad de grupos habilitados
     * @return detalle del momento
     */
    public static MomentDetailResponse toDetail(Moment moment, LocalDate today, int groupCount) {
        List<EvaluatorWeightItemResponse> weights = moment.getEvaluatorWeights().stream()
                .map(w -> new EvaluatorWeightItemResponse(
                        w.getEvaluatorType().getId(),
                        w.getEvaluatorType().getName(),
                        w.getWeight()))
                .toList();

        return new MomentDetailResponse(
                toSummary(moment, today, groupCount),
                moment.getTeacherWeight(),
                weights,
                moment.getTotalWeight(),
                moment.getMaxScore(),
                moment.getLearningOutcomes().stream().map(MomentMapper::toLearningOutcome).toList());
    }

    public static LearningOutcomeResponse toLearningOutcome(LearningOutcome outcome) {
        return new LearningOutcomeResponse(outcome.getId(), outcome.getCode(), outcome.getDescription(),
                outcome.getMaxScore(), outcome.getCriteria().stream().map(MomentMapper::toCriterion).toList());
    }

    public static CriterionResponse toCriterion(EvaluationCriterion criterion) {
        return new CriterionResponse(
                criterion.getId(),
                criterion.getDescription(),
                criterion.getDisplayOrder(),
                criterion.getMaxScore(),
                criterion.getLevels().stream().map(MomentMapper::toLevel).toList());
    }

    public static LevelResponse toLevel(PerformanceLevel level) {
        return new LevelResponse(
                level.getId(),
                level.getName(),
                level.getDescription(),
                level.getScore(),
                level.getDisplayOrder());
    }
}
