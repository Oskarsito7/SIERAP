package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest.EvaluatorWeightItemRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de {@link MomentServiceImpl}, verificando consulta, creación,
 * actualización parcial, eliminación y configuración de pesos de momentos.
 */
@ExtendWith(MockitoExtension.class)
class MomentServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Mock
    private ProjectGroupRepository projectGroupRepository;

    @Mock
    private AccessScope accessScope;

    private MomentServiceImpl momentService;

    private AcademicPeriod period;
    private Faculty faculty;
    private Program program;
    private Student student;
    private Admin admin;
    private AuthenticatedUser studentActor;
    private AuthenticatedUser adminActor;

    @BeforeEach
    void setUp() {
        momentService = new MomentServiceImpl(momentRepository, academicPeriodRepository,
                programRepository, evaluatorTypeRepository, projectGroupRepository,
                accessScope, FIXED_CLOCK);

        period = new AcademicPeriod("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 11, 28));
        ReflectionTestUtils.setField(period, "id", 1L);

        faculty = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(faculty, "id", 10L);

        program = new Program("108", "Sistemas");
        ReflectionTestUtils.setField(program, "id", 100L);
        faculty.addProgram(program);

        student = new Student("Juan", "Pérez", DocumentType.CC, "12345", "juan@unicesar.edu.co",
                "hash", LocalDateTime.now(FIXED_CLOCK), "20261001", 2, program);
        ReflectionTestUtils.setField(student, "id", 50L);

        admin = new Admin("Carlos", "Ramos", DocumentType.CC, "67890", "admin@unicesar.edu.co",
                "hash", LocalDateTime.now(FIXED_CLOCK), faculty);
        ReflectionTestUtils.setField(admin, "id", 60L);

        studentActor = new AuthenticatedUser(50L, "juan@unicesar.edu.co", UserRole.STUDENT, false);
        adminActor = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
    }

    @Test
    void listPassesStudentIdToFindForStudent() {
        when(academicPeriodRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.of(period));
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findForStudent(1L, 100L, 2, 50L)).thenReturn(List.of(moment));

        List<MomentSummaryResponse> result = momentService.list(studentActor, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
        assertThat(result.get(0).groupCount()).isZero();
        verify(momentRepository).findForStudent(1L, 100L, 2, 50L);
    }

    @Test
    void studentCanViewMomentInTheirSemesterRange() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(projectGroupRepository.countByMomentId(200L)).thenReturn(4);

        MomentDetailResponse response = momentService.get(studentActor, 200L);

        assertThat(response.summary().id()).isEqualTo(200L);
        assertThat(response.summary().groupCount()).isEqualTo(4);
    }

    @Test
    void studentCanViewMomentOutsideSemesterRangeIfTheyHaveAGroup() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 201L);

        when(momentRepository.findById(201L)).thenReturn(Optional.of(moment));
        when(projectGroupRepository.existsByMomentIdAndMembersId(201L, 50L)).thenReturn(true);
        when(projectGroupRepository.countByMomentId(201L)).thenReturn(1);

        MomentDetailResponse response = momentService.get(studentActor, 201L);

        assertThat(response.summary().id()).isEqualTo(201L);
        verify(projectGroupRepository).existsByMomentIdAndMembersId(201L, 50L);
    }

    @Test
    void studentCannotViewMomentOutsideSemesterRangeWithoutGroup() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 201L);

        when(momentRepository.findById(201L)).thenReturn(Optional.of(moment));
        when(projectGroupRepository.existsByMomentIdAndMembersId(201L, 50L)).thenReturn(false);

        assertThatThrownBy(() -> momentService.get(studentActor, 201L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("No tiene acceso a este momento.");
    }

    @Test
    void createMomentSuccessfully() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 500L);
            return m;
        });

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 28));

        MomentDetailResponse response = momentService.create(adminActor, request);

        assertThat(response.summary().id()).isEqualTo(500L);
        assertThat(response.summary().name()).isEqualTo("Momento 1");
        assertThat(response.teacherWeight()).isEqualByComparingTo("100.00");
        assertThat(response.totalWeight()).isEqualByComparingTo("100.00");
        assertThat(response.evaluatorWeights()).isEmpty();
    }

    @Test
    void createMomentThrowsConflictWhenNumberAlreadyExists() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(true);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createMomentThrowsWhenSemesterRangeIsInvalid() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 5, 3, "Bajo", 3, 5,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El semestre inicial no puede ser mayor que el semestre final.");
    }

    @Test
    void createMomentThrowsWhenMemberRangeIsInvalid() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 6, 5,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mínimo de integrantes no puede ser mayor que el máximo.");
    }

    @Test
    void createMomentThrowsWhenWindowIsIncomplete() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                LocalDate.of(2026, 8, 10), null, null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La ventana de conformación de grupos debe tener fecha de inicio y de fin.");
    }

    @Test
    void createMomentThrowsWhenWindowStartAfterEnd() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 10), null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La ventana de conformación de grupos no puede terminar antes de empezar.");
    }

    @Test
    void createMomentThrowsWhenWindowsOverlap() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20), null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La ventana de entregas debe empezar después de la de conformación de grupos.");
    }

    @Test
    void createMomentThrowsWhenGradingEndAfterPeriodUpcDeadline() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(100L, 1L, 1)).thenReturn(false);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                null, null, null, null,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 30));

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("límite de evaluación de la UPC");
    }

    @Test
    void createMomentThrowsForbiddenWhenOutOfScope() {
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(admin, 10L);

        NewMomentRequest request = new NewMomentRequest(
                100L, 1L, 1, "Momento 1", "Programación", 1, 3, "Bajo", 3, 5,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> momentService.create(adminActor, request))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void updateMomentSuccessfully() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.countByMomentId(200L)).thenReturn(2);

        UpdateMomentRequest request = new UpdateMomentRequest(
                null, null, "Momento 1 Editado", "Nueva Asignatura", null, null,
                null, null, null,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20),
                null, null, null, null,
                true, false, false);

        MomentDetailResponse response = momentService.update(adminActor, 200L, request);

        assertThat(response.summary().name()).isEqualTo("Momento 1 Editado");
        assertThat(response.summary().subject()).isEqualTo("Nueva Asignatura");
        assertThat(response.summary().groupFormationStart()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(response.summary().groupFormationEnd()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(response.summary().groupCount()).isEqualTo(2);
    }

    @Test
    void updateMomentClearsWindowWhenBothDatesAreNull() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        UpdateMomentRequest request = new UpdateMomentRequest(
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null, null,
                true, false, false);

        MomentDetailResponse response = momentService.update(adminActor, 200L, request);

        assertThat(response.summary().groupFormationStart()).isNull();
        assertThat(response.summary().groupFormationEnd()).isNull();
    }

    @Test
    void updateMomentThrowsConflictWhenNumberCollides() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumberAndIdNot(100L, 1L, 2, 200L)).thenReturn(true);

        UpdateMomentRequest request = new UpdateMomentRequest(
                null, 2, null, null, null, null,
                null, null, null,
                null, null, null, null, null, null,
                false, false, false);

        assertThatThrownBy(() -> momentService.update(adminActor, 200L, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateMomentThrowsWhenMomentAlreadyLaunched() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 1));
        moment.setGradingStart(LocalDate.of(2026, 10, 2));
        moment.setGradingEnd(LocalDate.of(2026, 10, 15));

        LearningOutcome lo = new LearningOutcome("RAP1", "Resultado");
        EvaluationCriterion c = new EvaluationCriterion("Criterio", 1);
        c.addLevel(new PerformanceLevel("Alto", "Desempeño", BigDecimal.valueOf(5.0), 1));
        lo.addCriterion(c);
        moment.addLearningOutcome(lo);
        moment.launch();

        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        UpdateMomentRequest request = new UpdateMomentRequest(
                null, null, "Nuevo nombre", null, null, null,
                null, null, null, null, null, null, null, null, null,
                false, false, false);

        assertThatThrownBy(() -> momentService.update(adminActor, 200L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El momento ya fue lanzado: su configuración no se puede modificar.");
    }

    @Test
    void deleteMomentSuccessfully() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.existsByMomentId(200L)).thenReturn(false);

        momentService.delete(adminActor, 200L);

        verify(momentRepository).delete(moment);
    }

    @Test
    void deleteMomentThrowsConflictWhenMomentHasGroups() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.existsByMomentId(200L)).thenReturn(true);

        assertThatThrownBy(() -> momentService.delete(adminActor, 200L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteMomentThrowsWhenAlreadyLaunched() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 10, 1));
        moment.setGradingStart(LocalDate.of(2026, 10, 2));
        moment.setGradingEnd(LocalDate.of(2026, 10, 15));

        LearningOutcome lo = new LearningOutcome("RAP1", "Resultado");
        EvaluationCriterion c = new EvaluationCriterion("Criterio", 1);
        c.addLevel(new PerformanceLevel("Alto", "Desempeño", BigDecimal.valueOf(5.0), 1));
        lo.addCriterion(c);
        moment.addLearningOutcome(lo);
        moment.launch();

        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        assertThatThrownBy(() -> momentService.delete(adminActor, 200L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El momento ya fue lanzado: su configuración no se puede modificar.");
    }

    @Test
    void updateWeightsSuccessfully() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        EvaluatorType type1 = new EvaluatorType("Empresario", "Evaluador del sector productivo");
        ReflectionTestUtils.setField(type1, "id", 10L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluatorTypeRepository.findById(10L)).thenReturn(Optional.of(type1));

        MomentWeightsRequest request = new MomentWeightsRequest(
                BigDecimal.valueOf(60),
                List.of(new EvaluatorWeightItemRequest(10L, BigDecimal.valueOf(40))));

        MomentWeightsResponse response = momentService.updateWeights(adminActor, 200L, request);

        assertThat(response.teacherWeight()).isEqualByComparingTo("60");
        assertThat(response.totalWeight()).isEqualByComparingTo("100");
        assertThat(response.evaluatorWeights()).hasSize(1);
        assertThat(response.evaluatorWeights().get(0).evaluatorTypeId()).isEqualTo(10L);
        assertThat(response.evaluatorWeights().get(0).evaluatorTypeName()).isEqualTo("Empresario");
        assertThat(response.evaluatorWeights().get(0).weight()).isEqualByComparingTo("40");
    }

    @Test
    void updateWeightsThrowsWhenDuplicateEvaluatorType() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        MomentWeightsRequest request = new MomentWeightsRequest(
                BigDecimal.valueOf(60),
                List.of(new EvaluatorWeightItemRequest(10L, BigDecimal.valueOf(20)),
                        new EvaluatorWeightItemRequest(10L, BigDecimal.valueOf(20))));

        assertThatThrownBy(() -> momentService.updateWeights(adminActor, 200L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Un tipo de evaluador no puede aparecer más de una vez.");
    }

    @Test
    void updateWeightsThrowsWhenEvaluatorTypeNotFound() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(evaluatorTypeRepository.findById(99L)).thenReturn(Optional.empty());

        MomentWeightsRequest request = new MomentWeightsRequest(
                BigDecimal.valueOf(60),
                List.of(new EvaluatorWeightItemRequest(99L, BigDecimal.valueOf(40))));

        assertThatThrownBy(() -> momentService.updateWeights(adminActor, 200L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
