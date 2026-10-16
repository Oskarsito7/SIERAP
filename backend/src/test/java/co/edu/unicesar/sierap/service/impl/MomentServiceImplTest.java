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

import co.edu.unicesar.sierap.dto.request.CopyMomentsRequest;
import co.edu.unicesar.sierap.dto.request.LaunchMomentRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest;
import co.edu.unicesar.sierap.dto.request.MomentWeightsRequest.EvaluatorWeightItemRequest;
import co.edu.unicesar.sierap.dto.request.NewMomentRequest;
import co.edu.unicesar.sierap.dto.request.UpdateMomentRequest;
import co.edu.unicesar.sierap.dto.response.LaunchCheckResponse;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.dto.response.MomentWeightsResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
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

    private Moment createFullyConfiguredMoment() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);
        moment.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        moment.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        moment.setSubmissionStart(LocalDate.of(2026, 9, 1));
        moment.setSubmissionEnd(LocalDate.of(2026, 9, 15));
        moment.setGradingStart(LocalDate.of(2026, 10, 1));
        moment.setGradingEnd(LocalDate.of(2026, 10, 20));

        LearningOutcome rap = new LearningOutcome("RAP1", "Resultado de aprendizaje");
        EvaluationCriterion crit = new EvaluationCriterion("Criterio 1", 1);
        PerformanceLevel level = new PerformanceLevel("Alto", "Excelente", BigDecimal.valueOf(5), 1);
        crit.addLevel(level);
        rap.addCriterion(crit);
        moment.addLearningOutcome(rap);
        return moment;
    }

    @Test
    void launchCheckWhenMomentAlreadyLaunched() {
        Moment moment = createFullyConfiguredMoment();
        ReflectionTestUtils.setField(moment, "status", co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        LaunchCheckResponse response = momentService.launchCheck(adminActor, 200L);

        assertThat(response.momentId()).isEqualTo(200L);
        assertThat(response.canLaunch()).isFalse();
        assertThat(response.problems()).containsExactly("El momento ya fue lanzado.");
    }

    @Test
    void launchCheckWhenIncompleteConfiguration() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.findByMomentIdOrderByName(200L)).thenReturn(List.of());

        LaunchCheckResponse response = momentService.launchCheck(adminActor, 200L);

        assertThat(response.canLaunch()).isFalse();
        assertThat(response.problems()).contains(
                "Para lanzar el momento se deben definir las tres ventanas de fechas.",
                "El momento debe tener al menos un RAP.");
    }

    @Test
    void launchCheckWhenValidWithWarnings() {
        Moment moment = createFullyConfiguredMoment();

        Teacher teacher = new Teacher("Marta", "Díaz", DocumentType.CC, "12345", "marta@unicesar.edu.co", "hash", LocalDateTime.now(), faculty);
        ReflectionTestUtils.setField(teacher, "id", 12L);

        ProjectGroup group1 = new ProjectGroup(moment, teacher, "Grupo 1");
        ReflectionTestUtils.setField(group1, "id", 1L);
        group1.addMember(student);

        EvaluatorType type2 = new EvaluatorType("Egresado", "Evaluador egresado");
        ReflectionTestUtils.setField(type2, "id", 2L);
        ExternalEvaluator extEval = new ExternalEvaluator("Carlos", "Gómez", DocumentType.CC, "9876", "carlos@gmail.com", "hash", LocalDateTime.now(), type2);
        ReflectionTestUtils.setField(extEval, "id", 50L);
        group1.addEvaluator(extEval);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.findByMomentIdOrderByName(200L)).thenReturn(List.of(group1));

        LaunchCheckResponse response = momentService.launchCheck(adminActor, 200L);

        assertThat(response.canLaunch()).isTrue();
        assertThat(response.problems()).isEmpty();
        assertThat(response.minMembers()).isEqualTo(3);
        assertThat(response.groupsBelowMinimum()).hasSize(1);
        assertThat(response.groupsBelowMinimum().get(0).name()).isEqualTo("Grupo 1");
        assertThat(response.groupsBelowMinimum().get(0).memberCount()).isEqualTo(1);
        assertThat(response.groupsBelowMinimum().get(0).teacher().fullName()).isEqualTo("Marta Díaz");

        assertThat(response.evaluatorTypesWithoutWeight()).hasSize(1);
        assertThat(response.evaluatorTypesWithoutWeight().get(0).evaluatorTypeId()).isEqualTo(2L);
        assertThat(response.evaluatorTypesWithoutWeight().get(0).evaluatorTypeName()).isEqualTo("Egresado");
        assertThat(response.evaluatorTypesWithoutWeight().get(0).evaluatorCount()).isEqualTo(1);
    }

    @Test
    void launchThrowsWhenAlreadyLaunched() {
        Moment moment = createFullyConfiguredMoment();
        ReflectionTestUtils.setField(moment, "status", co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        assertThatThrownBy(() -> momentService.launch(adminActor, 200L, new LaunchMomentRequest(false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El momento ya fue lanzado: su configuración no se puede modificar.");
    }

    @Test
    void launchThrowsWhenBlockersExist() {
        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);

        assertThatThrownBy(() -> momentService.launch(adminActor, 200L, new LaunchMomentRequest(false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Para lanzar el momento se deben definir las tres ventanas de fechas.");
    }

    @Test
    void launchThrowsWhenUnconfirmedUnderMinimum() {
        Moment moment = createFullyConfiguredMoment();

        Teacher teacher = new Teacher("Marta", "Díaz", DocumentType.CC, "12345", "marta@unicesar.edu.co", "hash", LocalDateTime.now(), faculty);
        ReflectionTestUtils.setField(teacher, "id", 12L);

        ProjectGroup group1 = new ProjectGroup(moment, teacher, "Grupo 1");
        ReflectionTestUtils.setField(group1, "id", 1L);
        group1.addMember(student);

        ProjectGroup group2 = new ProjectGroup(moment, teacher, "Grupo 2");
        ReflectionTestUtils.setField(group2, "id", 2L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.findByMomentIdOrderByName(200L)).thenReturn(List.of(group1, group2));

        assertThatThrownBy(() -> momentService.launch(adminActor, 200L, new LaunchMomentRequest(false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Hay 2 grupos por debajo del mínimo de 3 integrantes (Grupo 1, Grupo 2). Confirme para lanzar de todos modos.");
    }

    @Test
    void launchSuccessfullyWhenConfirmedUnderMinimum() {
        Moment moment = createFullyConfiguredMoment();

        Teacher teacher = new Teacher("Marta", "Díaz", DocumentType.CC, "12345", "marta@unicesar.edu.co", "hash", LocalDateTime.now(), faculty);
        ReflectionTestUtils.setField(teacher, "id", 12L);

        ProjectGroup group1 = new ProjectGroup(moment, teacher, "Grupo 1");
        ReflectionTestUtils.setField(group1, "id", 1L);
        group1.addMember(student);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.findByMomentIdOrderByName(200L)).thenReturn(List.of(group1));
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MomentDetailResponse response = momentService.launch(adminActor, 200L, new LaunchMomentRequest(true));

        assertThat(response.summary().status()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);
        assertThat(moment.getStatus()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);
    }

    @Test
    void launchSuccessfullyWithoutUnderMinimum() {
        Moment moment = createFullyConfiguredMoment();

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(projectGroupRepository.findByMomentIdOrderByName(200L)).thenReturn(List.of());
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MomentDetailResponse response = momentService.launch(adminActor, 200L, null);

        assertThat(response.summary().status()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);
        assertThat(moment.getStatus()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.LAUNCHED);
    }

    @Test
    void launchCheckThrowsWhenOutOfScope() {
        Moment moment = createFullyConfiguredMoment();

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(admin, faculty.getId());

        assertThatThrownBy(() -> momentService.launchCheck(adminActor, 200L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("Solo puede gestionar información de su facultad.");
    }

    @Test
    void copyThrowsWhenSamePeriod() {
        CopyMomentsRequest request = new CopyMomentsRequest(1L, 1L, null);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El periodo de origen y de destino no pueden ser el mismo.");
    }

    @Test
    void copyThrowsWhenPeriodNotFound() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.empty());

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, null);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void copyThrowsWhenProgramNotFound() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, 999L);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void copyThrowsWhenProgramOutOfScope() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(admin, faculty.getId());

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, 100L);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("Solo puede gestionar información de su facultad.");
    }

    @Test
    void copyThrowsWhenNoSourceMomentsInScope() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(accessScope.facultyOf(admin)).thenReturn(faculty.getId());
        when(momentRepository.findByPeriodIdAndProgramFacultyIdOrderByProgramNameAscNumberAsc(1L, faculty.getId()))
                .thenReturn(List.of());

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, null);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El periodo de origen no tiene momentos configurados dentro del alcance.");
    }

    @Test
    void copyThrowsWhenCollisionsInDestination() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));
        ReflectionTestUtils.setField(period2, "id", 2L);
        Moment moment = createFullyConfiguredMoment();

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(accessScope.facultyOf(admin)).thenReturn(faculty.getId());
        when(momentRepository.findByPeriodIdAndProgramFacultyIdOrderByProgramNameAscNumberAsc(1L, faculty.getId()))
                .thenReturn(List.of(moment));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(program.getId(), 2L, 1))
                .thenReturn(true);

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, null);

        assertThatThrownBy(() -> momentService.copy(adminActor, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se pudo copiar la configuración: ya existen momentos en el periodo destino (Sistemas (Momento 1)).");
    }

    @Test
    void copySuccessfullyWithRubricAndWeights() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));
        ReflectionTestUtils.setField(period2, "id", 2L);

        Moment source = createFullyConfiguredMoment();
        EvaluatorType type = new EvaluatorType("Empresario", "Sector productivo");
        ReflectionTestUtils.setField(type, "id", 10L);
        source.setTeacherWeight(BigDecimal.valueOf(60));
        source.setEvaluatorWeight(type, BigDecimal.valueOf(40));

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(adminActor)).thenReturn(admin);
        when(programRepository.findById(100L)).thenReturn(Optional.of(program));
        when(momentRepository.findByPeriodIdAndProgramIdOrderByNumberAsc(1L, 100L)).thenReturn(List.of(source));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(program.getId(), 2L, 1)).thenReturn(false);
        when(momentRepository.save(any(Moment.class))).thenAnswer(inv -> {
            Moment m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 300L);
            return m;
        });

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, 100L);
        List<MomentSummaryResponse> result = momentService.copy(adminActor, request);

        assertThat(result).hasSize(1);
        MomentSummaryResponse summary = result.get(0);
        assertThat(summary.id()).isEqualTo(300L);
        assertThat(summary.name()).isEqualTo("Momento 1");
        assertThat(summary.status()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.CONFIGURATION);
        assertThat(summary.groupCount()).isEqualTo(0);
        assertThat(summary.groupFormationStart()).isNull();
        assertThat(summary.submissionStart()).isNull();
        assertThat(summary.gradingStart()).isNull();

        org.mockito.ArgumentCaptor<Moment> captor = org.mockito.ArgumentCaptor.forClass(Moment.class);
        verify(momentRepository).save(captor.capture());
        Moment copied = captor.getValue();

        assertThat(copied.getPeriod()).isEqualTo(period2);
        assertThat(copied.getStatus()).isEqualTo(co.edu.unicesar.sierap.entity.enums.MomentStatus.CONFIGURATION);
        assertThat(copied.getTeacherWeight()).isEqualByComparingTo("60");
        assertThat(copied.getEvaluatorWeights()).hasSize(1);
        assertThat(copied.getEvaluatorWeights().get(0).getWeight()).isEqualByComparingTo("40");
        assertThat(copied.getLearningOutcomes()).hasSize(1);
        LearningOutcome copiedRap = copied.getLearningOutcomes().get(0);
        assertThat(copiedRap.getCode()).isEqualTo("RAP1");
        assertThat(copiedRap.getCriteria()).hasSize(1);
        EvaluationCriterion copiedCrit = copiedRap.getCriteria().get(0);
        assertThat(copiedCrit.getDescription()).isEqualTo("Criterio 1");
        assertThat(copiedCrit.getLevels()).hasSize(1);
        PerformanceLevel copiedLevel = copiedCrit.getLevels().get(0);
        assertThat(copiedLevel.getName()).isEqualTo("Alto");
        assertThat(copiedLevel.getScore()).isEqualByComparingTo("5");
    }

    @Test
    void copySuccessfullyForSuperAdmin() {
        AcademicPeriod period2 = new AcademicPeriod("2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 10));
        ReflectionTestUtils.setField(period2, "id", 2L);

        Moment source = createFullyConfiguredMoment();
        SuperAdmin superAdmin = new SuperAdmin("Admin", "General", DocumentType.CC, "1000", "super@unicesar.edu.co", "hash", LocalDateTime.now());
        AuthenticatedUser superActor = new AuthenticatedUser(1L, "super@unicesar.edu.co", UserRole.SUPER_ADMIN, false);

        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(accessScope.loadActor(superActor)).thenReturn(superAdmin);
        when(momentRepository.findByPeriodIdOrderByProgramNameAscNumberAsc(1L)).thenReturn(List.of(source));
        when(momentRepository.existsByProgramIdAndPeriodIdAndNumber(program.getId(), 2L, 1)).thenReturn(false);
        when(momentRepository.save(any(Moment.class))).thenAnswer(inv -> inv.getArgument(0));

        CopyMomentsRequest request = new CopyMomentsRequest(1L, 2L, null);
        List<MomentSummaryResponse> result = momentService.copy(superActor, request);

        assertThat(result).hasSize(1);
    }
}
