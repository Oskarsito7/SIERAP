package co.edu.unicesar.sierap.service.impl;

import java.math.BigDecimal;

import co.edu.unicesar.sierap.dto.request.NewCriterionRequest;
import co.edu.unicesar.sierap.dto.request.NewLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.NewLevelRequest;
import co.edu.unicesar.sierap.dto.request.UpdateCriterionRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLevelRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LearningOutcomeResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.MomentMapper;
import co.edu.unicesar.sierap.repository.EvaluationCriterionRepository;
import co.edu.unicesar.sierap.repository.LearningOutcomeRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.PerformanceLevelRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.RubricService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link RubricService}.
 *
 * <p>Permite gestionar RAP, criterios y niveles de desempeño solo mientras el momento
 * dueño se encuentre en estado {@link MomentStatus#CONFIGURATION} (ADR-012).</p>
 */
@Service
@Transactional
public class RubricServiceImpl implements RubricService {

    private final MomentRepository momentRepository;
    private final LearningOutcomeRepository learningOutcomeRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final PerformanceLevelRepository performanceLevelRepository;
    private final AccessScope accessScope;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param momentRepository              repositorio de momentos
     * @param learningOutcomeRepository     repositorio de RAP
     * @param evaluationCriterionRepository repositorio de criterios
     * @param performanceLevelRepository    repositorio de niveles
     * @param accessScope                   reglas de alcance
     */
    public RubricServiceImpl(MomentRepository momentRepository,
                             LearningOutcomeRepository learningOutcomeRepository,
                             EvaluationCriterionRepository evaluationCriterionRepository,
                             PerformanceLevelRepository performanceLevelRepository,
                             AccessScope accessScope) {
        this.momentRepository = momentRepository;
        this.learningOutcomeRepository = learningOutcomeRepository;
        this.evaluationCriterionRepository = evaluationCriterionRepository;
        this.performanceLevelRepository = performanceLevelRepository;
        this.accessScope = accessScope;
    }

    @Override
    public LearningOutcomeResponse createLearningOutcome(AuthenticatedUser actor, Long momentId,
                                                         NewLearningOutcomeRequest request) {
        User user = accessScope.loadActor(actor);
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Momento", momentId));
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        String code = request.code().trim();
        String description = request.description().trim();

        if (learningOutcomeRepository.existsByMomentIdAndCode(moment.getId(), code)) {
            throw new DataIntegrityViolationException(
                    "Ya existe un RAP con el código " + code + " en este momento.");
        }

        LearningOutcome outcome = new LearningOutcome(code, description);
        moment.addLearningOutcome(outcome);
        LearningOutcome saved = learningOutcomeRepository.save(outcome);
        return MomentMapper.toLearningOutcome(saved);
    }

    @Override
    public LearningOutcomeResponse updateLearningOutcome(AuthenticatedUser actor, Long id,
                                                         UpdateLearningOutcomeRequest request) {
        User user = accessScope.loadActor(actor);
        LearningOutcome outcome = learningOutcomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RAP", id));
        Moment moment = outcome.getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        if (request.code() != null) {
            String code = request.code().trim();
            if (!code.equals(outcome.getCode()) && learningOutcomeRepository.existsByMomentIdAndCodeAndIdNot(
                    moment.getId(), code, outcome.getId())) {
                throw new DataIntegrityViolationException(
                        "Ya existe un RAP con el código " + code + " en este momento.");
            }
            outcome.setCode(code);
        }
        if (request.description() != null) {
            outcome.setDescription(request.description().trim());
        }

        return MomentMapper.toLearningOutcome(outcome);
    }

    @Override
    public void deleteLearningOutcome(AuthenticatedUser actor, Long id) {
        User user = accessScope.loadActor(actor);
        LearningOutcome outcome = learningOutcomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RAP", id));
        Moment moment = outcome.getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        moment.removeLearningOutcome(outcome);
        learningOutcomeRepository.delete(outcome);
    }

    @Override
    public CriterionResponse createCriterion(AuthenticatedUser actor, Long outcomeId,
                                            NewCriterionRequest request) {
        User user = accessScope.loadActor(actor);
        LearningOutcome outcome = learningOutcomeRepository.findById(outcomeId)
                .orElseThrow(() -> new ResourceNotFoundException("RAP", outcomeId));
        Moment moment = outcome.getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        int order = request.displayOrder() != null
                ? request.displayOrder()
                : outcome.getCriteria().stream().mapToInt(EvaluationCriterion::getDisplayOrder).max().orElse(0) + 1;

        EvaluationCriterion criterion = new EvaluationCriterion(request.description().trim(), order);
        outcome.addCriterion(criterion);
        EvaluationCriterion saved = evaluationCriterionRepository.save(criterion);
        return MomentMapper.toCriterion(saved);
    }

    @Override
    public CriterionResponse updateCriterion(AuthenticatedUser actor, Long id,
                                            UpdateCriterionRequest request) {
        User user = accessScope.loadActor(actor);
        EvaluationCriterion criterion = evaluationCriterionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criterio", id));
        Moment moment = criterion.getLearningOutcome().getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        if (request.description() != null) {
            criterion.setDescription(request.description().trim());
        }
        if (request.displayOrder() != null) {
            criterion.setDisplayOrder(request.displayOrder());
        }

        return MomentMapper.toCriterion(criterion);
    }

    @Override
    public void deleteCriterion(AuthenticatedUser actor, Long id) {
        User user = accessScope.loadActor(actor);
        EvaluationCriterion criterion = evaluationCriterionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criterio", id));
        Moment moment = criterion.getLearningOutcome().getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        criterion.getLearningOutcome().removeCriterion(criterion);
        evaluationCriterionRepository.delete(criterion);
    }

    @Override
    public LevelResponse createLevel(AuthenticatedUser actor, Long criterionId,
                                     NewLevelRequest request) {
        User user = accessScope.loadActor(actor);
        EvaluationCriterion criterion = evaluationCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterio", criterionId));
        Moment moment = criterion.getLearningOutcome().getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        requireValidScore(request.score());

        int order = request.displayOrder() != null
                ? request.displayOrder()
                : criterion.getLevels().stream().mapToInt(PerformanceLevel::getDisplayOrder).max().orElse(0) + 1;

        String description = request.description() != null ? request.description().trim() : null;
        PerformanceLevel level = new PerformanceLevel(request.name().trim(), description, request.score(), order);
        criterion.addLevel(level);
        PerformanceLevel saved = performanceLevelRepository.save(level);
        return MomentMapper.toLevel(saved);
    }

    @Override
    public LevelResponse updateLevel(AuthenticatedUser actor, Long id,
                                     UpdateLevelRequest request) {
        User user = accessScope.loadActor(actor);
        PerformanceLevel level = performanceLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        Moment moment = level.getCriterion().getLearningOutcome().getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        if (request.name() != null) {
            level.setName(request.name().trim());
        }
        if (request.description() != null) {
            level.setDescription(request.description().trim());
        }
        if (request.score() != null) {
            requireValidScore(request.score());
            level.setScore(request.score());
        }
        if (request.displayOrder() != null) {
            level.setDisplayOrder(request.displayOrder());
        }

        return MomentMapper.toLevel(level);
    }

    @Override
    public void deleteLevel(AuthenticatedUser actor, Long id) {
        User user = accessScope.loadActor(actor);
        PerformanceLevel level = performanceLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        Moment moment = level.getCriterion().getLearningOutcome().getMoment();
        accessScope.requireFaculty(user, moment.getProgram().getFaculty().getId());
        requireConfiguration(moment);

        level.getCriterion().removeLevel(level);
        performanceLevelRepository.delete(level);
    }

    private void requireConfiguration(Moment moment) {
        if (moment.getStatus() == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException(
                    "El momento ya fue lanzado: su configuración no se puede modificar.");
        }
    }

    private void requireValidScore(BigDecimal score) {
        if (score == null || score.signum() < 0 || score.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException(
                    "El puntaje debe ser mayor o igual a 0, con dos decimales como máximo.");
        }
    }
}
