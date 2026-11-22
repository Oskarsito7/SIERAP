package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.ExtendMomentRequest;
import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de la prórroga excepcional de un momento lanzado (ADR-023), con
 * el reloj fijo: reglas de {@link Moment#extend} a través de
 * {@link MomentServiceImpl#extend} y registro en la bitácora.
 *
 * <p>El momento de prueba tiene entregas hasta el 1 de octubre, calificación del 2 al
 * 15 de octubre y un límite de evaluación de la UPC el 28 de noviembre de 2026.</p>
 */
@ExtendWith(MockitoExtension.class)
class MomentExtensionTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));
    private static final String REASON = "Paro estudiantil de dos semanas";

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

    @Mock
    private AuditService auditService;

    private MomentServiceImpl service;
    private AcademicPeriod period;
    private Program program;
    private Admin admin;
    private AuthenticatedUser actor;
    private Moment moment;

    @BeforeEach
    void setUp() {
        service = new MomentServiceImpl(momentRepository, academicPeriodRepository, programRepository,
                evaluatorTypeRepository, projectGroupRepository, accessScope, auditService, FIXED_CLOCK);
        period = new AcademicPeriod("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 11, 28));
        Faculty faculty = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(faculty, "id", 10L);
        program = new Program("108", "Sistemas");
        faculty.addProgram(program);
        admin = new Admin("Carlos", "Ramos", DocumentType.CC, "67890", "admin@unicesar.edu.co", "hash",
                LocalDateTime.now(FIXED_CLOCK), faculty);
        actor = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        moment = launchedMoment();
        lenient().when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));
        lenient().when(accessScope.loadActor(actor)).thenReturn(admin);
    }

    @Test
    void extendsOnlyTheGradingEnd() {
        MomentDetailResponse response = service.extend(actor, 200L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 11, 20), REASON));

        assertThat(moment.getGradingEnd()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(moment.getSubmissionEnd()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(moment.getGradingStart()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(response.summary().gradingEnd()).isEqualTo(LocalDate.of(2026, 11, 20));
        verify(auditService).record(eq(AuditAction.MOMENT_EXTENDED), eq("Moment"), eq(200L), eq(10L),
                eq("Prorrogó el Momento 1 de Sistemas (2026-2): fin de calificación 2026-10-15 a 2026-11-20."
                        + " Motivo: " + REASON));
    }

    @Test
    void extendingSubmissionsShiftsTheGradingStartWhenNeeded() {
        service.extend(actor, 200L, new ExtendMomentRequest(LocalDate.of(2026, 10, 5), null, REASON));

        assertThat(moment.getSubmissionEnd()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(moment.getGradingStart()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(moment.getGradingEnd()).isEqualTo(LocalDate.of(2026, 10, 15));
        verify(auditService).record(eq(AuditAction.MOMENT_EXTENDED), eq("Moment"), eq(200L), eq(10L),
                eq("Prorrogó el Momento 1 de Sistemas (2026-2): fin de entregas 2026-10-01 a 2026-10-05;"
                        + " inicio de calificación 2026-10-02 a 2026-10-06. Motivo: " + REASON));
    }

    @Test
    void extendsBothDates() {
        service.extend(actor, 200L,
                new ExtendMomentRequest(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 28), REASON));

        assertThat(moment.getSubmissionEnd()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(moment.getGradingStart()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(moment.getGradingEnd()).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void extendingSubmissionsPastTheGradingEndAsksToExtendGradingToo() {
        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(LocalDate.of(2026, 10, 20), null, REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("amplíe también el fin de calificación");

        assertThat(moment.getSubmissionEnd()).isEqualTo(LocalDate.of(2026, 10, 1));
        verifyNoInteractions(auditService);
    }

    @Test
    void shorteningOrKeepingADateIsRejected() {
        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 10, 10), REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solo se pueden ampliar");
        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(LocalDate.of(2026, 10, 1), null, REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solo se pueden ampliar");

        assertThat(moment.getGradingEnd()).isEqualTo(LocalDate.of(2026, 10, 15));
        verifyNoInteractions(auditService);
    }

    @Test
    void passingTheUpcDeadlineIsRejected() {
        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 11, 29), REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La ventana de calificación debe terminar a más tardar el 2026-11-28,"
                        + " límite de evaluación de la UPC.");
        verifyNoInteractions(auditService);
    }

    @Test
    void aMomentThatIsNotLaunchedCannotBeExtended() {
        Moment configuring = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(configuring);
        ReflectionTestUtils.setField(configuring, "id", 201L);
        when(momentRepository.findById(201L)).thenReturn(Optional.of(configuring));

        assertThatThrownBy(() -> service.extend(actor, 201L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 11, 20), REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Solo se puede prorrogar un momento ya lanzado.");
        verifyNoInteractions(auditService);
    }

    @Test
    void atLeastOneDateIsRequired() {
        assertThatThrownBy(() -> service.extend(actor, 200L, new ExtendMomentRequest(null, null, REASON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Indique al menos una fecha para la prórroga.");
    }

    @Test
    void theReasonMustHaveAtLeastTenCharactersOnceTrimmed() {
        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 11, 20), "   corto    ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El motivo debe tener entre 10 y 500 caracteres.");
        verifyNoInteractions(auditService);
    }

    @Test
    void anAdminOfAnotherFacultyIsForbidden() {
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(any(), eq(10L));

        assertThatThrownBy(() -> service.extend(actor, 200L,
                new ExtendMomentRequest(null, LocalDate.of(2026, 11, 20), REASON)))
                .isInstanceOf(ForbiddenAccessException.class);
        assertThat(moment.getGradingEnd()).isEqualTo(LocalDate.of(2026, 10, 15));
        verifyNoInteractions(auditService);
    }

    private Moment launchedMoment() {
        Moment launched = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        launched.setGroupFormationStart(LocalDate.of(2026, 8, 10));
        launched.setGroupFormationEnd(LocalDate.of(2026, 8, 20));
        launched.setSubmissionStart(LocalDate.of(2026, 9, 1));
        launched.setSubmissionEnd(LocalDate.of(2026, 10, 1));
        launched.setGradingStart(LocalDate.of(2026, 10, 2));
        launched.setGradingEnd(LocalDate.of(2026, 10, 15));
        LearningOutcome outcome = new LearningOutcome("RAP1", "Resultado");
        EvaluationCriterion criterion = new EvaluationCriterion("Criterio", 1);
        criterion.addLevel(new PerformanceLevel("Alto", "Desempeño", BigDecimal.valueOf(5.0), 1));
        outcome.addCriterion(criterion);
        launched.addLearningOutcome(outcome);
        launched.launch();
        program.addMoment(launched);
        ReflectionTestUtils.setField(launched, "id", 200L);
        return launched;
    }
}
