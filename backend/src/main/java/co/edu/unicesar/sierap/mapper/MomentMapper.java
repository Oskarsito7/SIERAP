package co.edu.unicesar.sierap.mapper;

import java.time.LocalDate;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
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
 * <p>Recorre relaciones LAZY (programa, periodo, RAP, criterios y niveles), así que
 * se debe llamar dentro de una transacción.</p>
 */
public final class MomentMapper {

    private MomentMapper() {
    }

    /**
     * Convierte un momento en su resumen.
     *
     * @param moment momento
     * @param today  fecha actual, obtenida del {@code Clock}, para calcular qué
     *               ventanas están abiertas
     * @return resumen del momento
     */
    public static MomentSummaryResponse toSummary(Moment moment, LocalDate today) {
        return new MomentSummaryResponse(moment.getId(), moment.getNumber(), moment.getName(),
                moment.getSubject(), moment.getProgram().getId(), moment.getProgram().getName(),
                moment.getPeriod().getId(), moment.getPeriod().getCode(), moment.getSemesterFrom(),
                moment.getSemesterTo(), moment.getMinMembers(), moment.getMaxMembers(), moment.getStatus(),
                moment.getGroupFormationStart(), moment.getGroupFormationEnd(), moment.getSubmissionStart(),
                moment.getSubmissionEnd(), moment.getGradingStart(), moment.getGradingEnd(),
                moment.isInGroupFormation(today), moment.isInSubmission(today), moment.isInGrading(today));
    }

    /**
     * Convierte un momento en su detalle, con la estructura de evaluación.
     *
     * @param moment momento
     * @param today  fecha actual, obtenida del {@code Clock}
     * @return detalle del momento
     */
    public static MomentDetailResponse toDetail(Moment moment, LocalDate today) {
        return new MomentDetailResponse(toSummary(moment, today), moment.getMaxScore(),
                moment.getLearningOutcomes().stream().map(MomentMapper::toLearningOutcome).toList());
    }

    private static LearningOutcomeResponse toLearningOutcome(LearningOutcome outcome) {
        return new LearningOutcomeResponse(outcome.getId(), outcome.getCode(), outcome.getDescription(),
                outcome.getMaxScore(), outcome.getCriteria().stream().map(MomentMapper::toCriterion).toList());
    }

    private static CriterionResponse toCriterion(EvaluationCriterion criterion) {
        return new CriterionResponse(criterion.getId(), criterion.getDescription(),
                criterion.getLevels().stream().map(MomentMapper::toLevel).toList());
    }

    private static LevelResponse toLevel(PerformanceLevel level) {
        return new LevelResponse(level.getId(), level.getName(), level.getDescription(), level.getScore());
    }
}
