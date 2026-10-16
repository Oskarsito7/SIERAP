package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewCriterionRequest;
import co.edu.unicesar.sierap.dto.request.NewLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.NewLevelRequest;
import co.edu.unicesar.sierap.dto.request.UpdateCriterionRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLearningOutcomeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateLevelRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.CriterionResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LearningOutcomeResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse.LevelResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.EvaluationCriterionRepository;
import co.edu.unicesar.sierap.repository.LearningOutcomeRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.PerformanceLevelRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de {@link RubricServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class RubricServiceImplTest {

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private LearningOutcomeRepository learningOutcomeRepository;

    @Mock
    private EvaluationCriterionRepository evaluationCriterionRepository;

    @Mock
    private PerformanceLevelRepository performanceLevelRepository;

    @Mock
    private AccessScope accessScope;

    private RubricServiceImpl rubricService;

    private AcademicPeriod period;
    private Faculty faculty;
    private Program program;
    private Moment moment;
    private Admin admin;
    private AuthenticatedUser adminActor;

    @BeforeEach
    void setUp() {
        rubricService = new RubricServiceImpl(momentRepository, learningOutcomeRepository,
                evaluationCriterionRepository, performanceLevelRepository, accessScope);

        period = new AcademicPeriod("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 11, 28));
        ReflectionTestUtils.setField(period, "id", 1L);

        faculty = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(faculty, "id", 10L);

        program = new Program("108", "Sistemas");
        ReflectionTestUtils.setField(program, "id", 100L);
        faculty.addProgram(program);

        moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        admin = new Admin("Carlos", "Ramos", DocumentType.CC, "67890", "admin@unicesar.edu.co",
                "hash", LocalDateTime.now(), faculty);
        ReflectionTestUtils.setField(admin, "id", 60L);

        adminActor = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
    }

    @Test
    void createLearningOutcomeSuccessfully() {
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(learningOutcomeRepository.existsByMomentIdAndCode(200L, "RAP1")).thenReturn(false);
        when(learningOutcomeRepository.save(any(LearningOutcome.class))).thenAnswer(invocation -> {
            LearningOutcome outcome = invocation.getArgument(0);
            ReflectionTestUtils.setField(outcome, "id", 300L);
            return outcome;
        });

        NewLearningOutcomeRequest request = new NewLearningOutcomeRequest("RAP1", "Resultado de aprendizaje 1");

        LearningOutcomeResponse response = rubricService.createLearningOutcome(adminActor, 200L, request);

        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.code()).isEqualTo("RAP1");
        assertThat(response.description()).isEqualTo("Resultado de aprendizaje 1");
        assertThat(response.maxScore()).isEqualByComparingTo("0.0");
        assertThat(response.criteria()).isEmpty();
    }

    @Test
    void createLearningOutcomeThrowsConflictWhenDuplicateCode() {
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(learningOutcomeRepository.existsByMomentIdAndCode(200L, "RAP1")).thenReturn(true);

        NewLearningOutcomeRequest request = new NewLearningOutcomeRequest("RAP1", "Resultado de aprendizaje 1");

        assertThatThrownBy(() -> rubricService.createLearningOutcome(adminActor, 200L, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createLearningOutcomeThrowsWhenMomentLaunched() {
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 1));
        moment.setGradingStart(LocalDate.of(2026, 10, 2));
        moment.setGradingEnd(LocalDate.of(2026, 10, 15));

        LearningOutcome lo = new LearningOutcome("RAP0", "Previo");
        EvaluationCriterion c = new EvaluationCriterion("Crit", 1);
        c.addLevel(new PerformanceLevel("Alto", "Desc", BigDecimal.valueOf(5), 1));
        lo.addCriterion(c);
        moment.addLearningOutcome(lo);
        moment.launch();

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));

        NewLearningOutcomeRequest request = new NewLearningOutcomeRequest("RAP1", "Resultado 1");

        assertThatThrownBy(() -> rubricService.createLearningOutcome(adminActor, 200L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El momento ya fue lanzado: su configuración no se puede modificar.");
    }

    @Test
    void createLearningOutcomeThrowsForbiddenWhenOutOfScope() {
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(admin, 10L);

        NewLearningOutcomeRequest request = new NewLearningOutcomeRequest("RAP1", "Resultado 1");

        assertThatThrownBy(() -> rubricService.createLearningOutcome(adminActor, 200L, request))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void updateLearningOutcomeSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc original");
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(outcome, "id", 300L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(learningOutcomeRepository.findById(300L)).thenReturn(Optional.of(outcome));
        when(learningOutcomeRepository.existsByMomentIdAndCodeAndIdNot(200L, "RAP2", 300L)).thenReturn(false);

        UpdateLearningOutcomeRequest request = new UpdateLearningOutcomeRequest("RAP2", "Desc nueva");

        LearningOutcomeResponse response = rubricService.updateLearningOutcome(adminActor, 300L, request);

        assertThat(response.code()).isEqualTo("RAP2");
        assertThat(response.description()).isEqualTo("Desc nueva");
    }

    @Test
    void updateLearningOutcomeThrowsConflictWhenDuplicateCode() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc original");
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(outcome, "id", 300L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(learningOutcomeRepository.findById(300L)).thenReturn(Optional.of(outcome));
        when(learningOutcomeRepository.existsByMomentIdAndCodeAndIdNot(200L, "RAP_EXISTENTE", 300L)).thenReturn(true);

        UpdateLearningOutcomeRequest request = new UpdateLearningOutcomeRequest("RAP_EXISTENTE", null);

        assertThatThrownBy(() -> rubricService.updateLearningOutcome(adminActor, 300L, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteLearningOutcomeSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc original");
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(outcome, "id", 300L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(learningOutcomeRepository.findById(300L)).thenReturn(Optional.of(outcome));

        rubricService.deleteLearningOutcome(adminActor, 300L);

        verify(learningOutcomeRepository).delete(outcome);
        assertThat(moment.getLearningOutcomes()).doesNotContain(outcome);
    }

    @Test
    void createCriterionSuccessfullyWithExplicitOrder() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(outcome, "id", 300L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(learningOutcomeRepository.findById(300L)).thenReturn(Optional.of(outcome));
        when(evaluationCriterionRepository.save(any(EvaluationCriterion.class))).thenAnswer(invocation -> {
            EvaluationCriterion c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 400L);
            return c;
        });

        NewCriterionRequest request = new NewCriterionRequest("Identificación del problema", 2);

        CriterionResponse response = rubricService.createCriterion(adminActor, 300L, request);

        assertThat(response.id()).isEqualTo(400L);
        assertThat(response.description()).isEqualTo("Identificación del problema");
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.maxScore()).isEqualByComparingTo("0.0");
        assertThat(response.levels()).isEmpty();
    }

    @Test
    void createCriterionAutoAssignsNextDisplayOrderWhenOmitted() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        outcome.addCriterion(new EvaluationCriterion("Criterio 1", 1));
        outcome.addCriterion(new EvaluationCriterion("Criterio 2", 3));
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(outcome, "id", 300L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(learningOutcomeRepository.findById(300L)).thenReturn(Optional.of(outcome));
        when(evaluationCriterionRepository.save(any(EvaluationCriterion.class))).thenAnswer(invocation -> {
            EvaluationCriterion c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 401L);
            return c;
        });

        NewCriterionRequest request = new NewCriterionRequest("Criterio siguiente", null);

        CriterionResponse response = rubricService.createCriterion(adminActor, 300L, request);

        assertThat(response.displayOrder()).isEqualTo(4); // max (3) + 1
    }

    @Test
    void updateCriterionSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Desc original", 1);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(criterion, "id", 400L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluationCriterionRepository.findById(400L)).thenReturn(Optional.of(criterion));

        UpdateCriterionRequest request = new UpdateCriterionRequest("Desc modificada", 5);

        CriterionResponse response = rubricService.updateCriterion(adminActor, 400L, request);

        assertThat(response.description()).isEqualTo("Desc modificada");
        assertThat(response.displayOrder()).isEqualTo(5);
    }

    @Test
    void deleteCriterionSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Desc original", 1);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(criterion, "id", 400L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluationCriterionRepository.findById(400L)).thenReturn(Optional.of(criterion));

        rubricService.deleteCriterion(adminActor, 400L);

        verify(evaluationCriterionRepository).delete(criterion);
        assertThat(outcome.getCriteria()).doesNotContain(criterion);
    }

    @Test
    void createLevelSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Crit", 1);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(criterion, "id", 400L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluationCriterionRepository.findById(400L)).thenReturn(Optional.of(criterion));
        when(performanceLevelRepository.save(any(PerformanceLevel.class))).thenAnswer(invocation -> {
            PerformanceLevel l = invocation.getArgument(0);
            ReflectionTestUtils.setField(l, "id", 500L);
            return l;
        });

        NewLevelRequest request = new NewLevelRequest("Alto", "Desempeño esperado", BigDecimal.valueOf(0.48), 2);

        LevelResponse response = rubricService.createLevel(adminActor, 400L, request);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.name()).isEqualTo("Alto");
        assertThat(response.description()).isEqualTo("Desempeño esperado");
        assertThat(response.score()).isEqualByComparingTo("0.48");
        assertThat(response.displayOrder()).isEqualTo(2);
    }

    @Test
    void createLevelThrowsWhenScoreHasMoreThanTwoDecimals() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Crit", 1);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(criterion, "id", 400L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluationCriterionRepository.findById(400L)).thenReturn(Optional.of(criterion));

        NewLevelRequest request = new NewLevelRequest("Alto", null, new BigDecimal("0.485"), null);

        assertThatThrownBy(() -> rubricService.createLevel(adminActor, 400L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El puntaje debe ser mayor o igual a 0, con dos decimales como máximo.");
    }

    @Test
    void createLevelThrowsWhenScoreIsNegative() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Crit", 1);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(criterion, "id", 400L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluationCriterionRepository.findById(400L)).thenReturn(Optional.of(criterion));

        NewLevelRequest request = new NewLevelRequest("Bajo", null, new BigDecimal("-0.1"), null);

        assertThatThrownBy(() -> rubricService.createLevel(adminActor, 400L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El puntaje debe ser mayor o igual a 0, con dos decimales como máximo.");
    }

    @Test
    void updateLevelSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Crit", 1);
        PerformanceLevel level = new PerformanceLevel("Bajo", "Desc", BigDecimal.ZERO, 1);
        criterion.addLevel(level);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(level, "id", 500L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(performanceLevelRepository.findById(500L)).thenReturn(Optional.of(level));

        UpdateLevelRequest request = new UpdateLevelRequest("Básico", "Nueva desc", BigDecimal.valueOf(0.25), 2);

        LevelResponse response = rubricService.updateLevel(adminActor, 500L, request);

        assertThat(response.name()).isEqualTo("Básico");
        assertThat(response.description()).isEqualTo("Nueva desc");
        assertThat(response.score()).isEqualByComparingTo("0.25");
        assertThat(response.displayOrder()).isEqualTo(2);
    }

    @Test
    void deleteLevelSuccessfully() {
        LearningOutcome outcome = new LearningOutcome("RAP1", "Desc");
        EvaluationCriterion criterion = new EvaluationCriterion("Crit", 1);
        PerformanceLevel level = new PerformanceLevel("Bajo", "Desc", BigDecimal.ZERO, 1);
        criterion.addLevel(level);
        outcome.addCriterion(criterion);
        moment.addLearningOutcome(outcome);
        ReflectionTestUtils.setField(level, "id", 500L);

        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(performanceLevelRepository.findById(500L)).thenReturn(Optional.of(level));

        rubricService.deleteLevel(adminActor, 500L);

        verify(performanceLevelRepository).delete(level);
        assertThat(criterion.getLevels()).doesNotContain(level);
    }
}
