package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.ReminderSent;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.entity.enums.ReminderKind;
import co.edu.unicesar.sierap.repository.EvaluationRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.ReminderSentRepository;
import co.edu.unicesar.sierap.repository.SubmissionRepository;
import co.edu.unicesar.sierap.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de {@link ReminderServiceImpl} con el reloj fijado el 22/11/2026:
 * cada tipo de recordatorio, que solo se avise a quien corresponde y la idempotencia.
 */
@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {

    private static final String FRONTEND = "http://localhost:5173";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T12:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);

    @Mock
    private ProjectGroupRepository groupRepository;
    @Mock
    private MomentRepository momentRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private ReminderSentRepository reminderSentRepository;
    @Mock
    private EmailService emailService;

    private ReminderServiceImpl service;
    private Moment moment;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties("una-clave-de-pruebas-de-al-menos-32-bytes", 15, 7, false,
                72, 1, 5, FRONTEND, "unicesar.edu.co", "SIERAP <no-reply@unicesar.edu.co>");
        service = new ReminderServiceImpl(groupRepository, momentRepository, submissionRepository,
                evaluationRepository, reminderSentRepository, emailService, properties, CLOCK);
        moment = mock(Moment.class);
        lenient().when(moment.getId()).thenReturn(4L);
        lenient().when(moment.getName()).thenReturn("Momento 1");
        teacher = mock(Teacher.class);
        lenient().when(teacher.getId()).thenReturn(50L);
        lenient().when(teacher.isActive()).thenReturn(true);
        lenient().when(teacher.getEmail()).thenReturn("docente@unicesar.edu.co");
        lenient().when(teacher.getFirstNames()).thenReturn("Ana");
        // Por defecto no hay nada que recordar ni nada ya recordado.
        lenient().when(groupRepository.findBySubmissionDeadline(any(), any())).thenReturn(List.of());
        lenient().when(groupRepository.findByMomentStatusAndMomentGradingEndOrderById(any(), any()))
                .thenReturn(List.of());
        lenient().when(momentRepository.findByStatusAndGradingEnd(any(), any())).thenReturn(List.of());
        lenient().when(reminderSentRepository.existsByKindAndReferenceIdAndRecipientId(any(), anyLong(), anyLong()))
                .thenReturn(false);
    }

    // ---------------------------------------------------------------- cierre de entregas

    @Test
    void closingInTwoDaysRemindsEveryActiveMemberOfAGroupWithoutSubmissions() {
        Student active = student(1L, "uno@unicesar.edu.co", "Uno", true);
        Student other = student(2L, "dos@unicesar.edu.co", "Dos", true);
        Student inactive = student(3L, "tres@unicesar.edu.co", "Tres", false);
        ProjectGroup group = group(7L, Set.of(active, other, inactive));
        when(groupRepository.findBySubmissionDeadline(MomentStatus.LAUNCHED, TODAY.plusDays(2)))
                .thenReturn(List.of(group));
        when(submissionRepository.existsByProjectGroupId(7L)).thenReturn(false);

        int enqueued = service.enqueueDueReminders();

        assertThat(enqueued).isEqualTo(2);
        String body = sentBody("uno@unicesar.edu.co", "SIERAP: la entrega de tu grupo vence en dos días");
        assertThat(body).contains("Hola, Uno:", "24/11/2026", "Grupo A", "Momento 1",
                FRONTEND + "/my-group?groupId=7", "SIERAP · Universidad Popular del Cesar");
        sentBody("dos@unicesar.edu.co", "SIERAP: la entrega de tu grupo vence en dos días");
        verify(emailService, never()).send(eq("tres@unicesar.edu.co"), anyString(), anyString());
        verify(reminderSentRepository, times(2)).save(any(ReminderSent.class));
    }

    @Test
    void groupsThatAlreadySubmittedAreNotReminded() {
        ProjectGroup group = group(7L, Set.of(student(1L, "uno@unicesar.edu.co", "Uno", true)));
        when(groupRepository.findBySubmissionDeadline(MomentStatus.LAUNCHED, TODAY.plusDays(2)))
                .thenReturn(List.of(group));
        when(submissionRepository.existsByProjectGroupId(7L)).thenReturn(true);

        assertThat(service.enqueueDueReminders()).isZero();

        verifyNoInteractions(emailService);
    }

    @Test
    void theClosingReminderIsNotRepeatedForAMemberAlreadyReminded() {
        ProjectGroup group = group(7L, Set.of(student(1L, "uno@unicesar.edu.co", "Uno", true)));
        when(groupRepository.findBySubmissionDeadline(MomentStatus.LAUNCHED, TODAY.plusDays(2)))
                .thenReturn(List.of(group));
        when(reminderSentRepository.existsByKindAndReferenceIdAndRecipientId(ReminderKind.SUBMISSION_CLOSING, 7L, 1L))
                .thenReturn(true);

        assertThat(service.enqueueDueReminders()).isZero();

        verifyNoInteractions(emailService);
        verify(reminderSentRepository, never()).save(any());
    }

    // ------------------------------------------------------------ evaluación pendiente

    @Test
    void pendingEvaluationRemindsTheTeacherAndTheExternalEvaluatorWithTheirOwnLinks() {
        ExternalEvaluator external = external(60L, "empresario@correo.com", "Eva", true);
        ProjectGroup group = group(7L, Set.of());
        when(group.getEvaluators()).thenReturn(Set.of(external));
        when(groupRepository.findByMomentStatusAndMomentGradingEndOrderById(MomentStatus.LAUNCHED, TODAY.plusDays(1)))
                .thenReturn(List.of(group));
        when(evaluationRepository.existsByProjectGroupIdAndEvaluatorIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);

        assertThat(service.enqueueDueReminders()).isEqualTo(2);

        String teacherBody = sentBody("docente@unicesar.edu.co", "SIERAP: tienes una evaluación pendiente");
        assertThat(teacherBody).contains("Hola, Ana:", "23/11/2026", FRONTEND + "/groups/7/evaluation");
        String externalBody = sentBody("empresario@correo.com", "SIERAP: tienes una evaluación pendiente");
        assertThat(externalBody).contains("Hola, Eva:", FRONTEND + "/assigned-groups/7");
    }

    @Test
    void onlyEvaluatorsWhoHaveNotSubmittedAreReminded() {
        ExternalEvaluator external = external(60L, "empresario@correo.com", "Eva", true);
        ProjectGroup group = group(7L, Set.of());
        when(group.getEvaluators()).thenReturn(Set.of(external));
        when(groupRepository.findByMomentStatusAndMomentGradingEndOrderById(MomentStatus.LAUNCHED, TODAY.plusDays(1)))
                .thenReturn(List.of(group));
        // El docente ya envió su evaluación; el externo no.
        when(evaluationRepository.existsByProjectGroupIdAndEvaluatorIdAndStatus(7L, 50L, EvaluationStatus.SUBMITTED))
                .thenReturn(true);
        when(evaluationRepository.existsByProjectGroupIdAndEvaluatorIdAndStatus(7L, 60L, EvaluationStatus.SUBMITTED))
                .thenReturn(false);

        assertThat(service.enqueueDueReminders()).isEqualTo(1);

        verify(emailService).send(eq("empresario@correo.com"), anyString(), anyString());
        verify(emailService, never()).send(eq("docente@unicesar.edu.co"), anyString(), anyString());
    }

    @Test
    void inactiveEvaluatorsAreNotReminded() {
        ExternalEvaluator external = external(60L, "empresario@correo.com", "Eva", false);
        ProjectGroup group = group(7L, Set.of());
        when(group.getEvaluators()).thenReturn(Set.of(external));
        when(groupRepository.findByMomentStatusAndMomentGradingEndOrderById(MomentStatus.LAUNCHED, TODAY.plusDays(1)))
                .thenReturn(List.of(group));

        assertThat(service.enqueueDueReminders()).isEqualTo(1);

        verify(emailService, never()).send(eq("empresario@correo.com"), anyString(), anyString());
    }

    @Test
    void theEvaluationReminderIsNotRepeated() {
        ProjectGroup group = group(7L, Set.of());
        when(groupRepository.findByMomentStatusAndMomentGradingEndOrderById(MomentStatus.LAUNCHED, TODAY.plusDays(1)))
                .thenReturn(List.of(group));
        when(reminderSentRepository.existsByKindAndReferenceIdAndRecipientId(ReminderKind.EVALUATION_PENDING, 7L, 50L))
                .thenReturn(true);

        assertThat(service.enqueueDueReminders()).isZero();

        verifyNoInteractions(emailService);
    }

    // --------------------------------------------------------------- notas publicadas

    @Test
    void theDayAfterGradingEndsActiveStudentsAreToldTheirGradesAreAvailable() {
        Student active = student(1L, "uno@unicesar.edu.co", "Uno", true);
        Student inactive = student(3L, "tres@unicesar.edu.co", "Tres", false);
        ProjectGroup group = group(7L, Set.of(active, inactive));
        when(momentRepository.findByStatusAndGradingEnd(MomentStatus.LAUNCHED, TODAY.minusDays(1)))
                .thenReturn(List.of(moment));
        when(groupRepository.findByMomentIdOrderByName(4L)).thenReturn(List.of(group));

        assertThat(service.enqueueDueReminders()).isEqualTo(1);

        String body = sentBody("uno@unicesar.edu.co", "SIERAP: ya puedes ver tus notas");
        assertThat(body).contains("Hola, Uno:", "Momento 1", FRONTEND + "/my-grades?momentId=4");
        verify(emailService, never()).send(eq("tres@unicesar.edu.co"), anyString(), anyString());
        ArgumentCaptor<ReminderSent> saved = ArgumentCaptor.forClass(ReminderSent.class);
        verify(reminderSentRepository).save(saved.capture());
        assertThat(saved.getValue().getKind()).isEqualTo(ReminderKind.GRADES_PUBLISHED);
        assertThat(saved.getValue().getReferenceId()).isEqualTo(4L);
        assertThat(saved.getValue().getRecipientId()).isEqualTo(1L);
        assertThat(saved.getValue().getSentOn()).isEqualTo(TODAY);
    }

    @Test
    void theGradesNoticeIsNotRepeatedIfTheJobRunsTwice() {
        Student active = student(1L, "uno@unicesar.edu.co", "Uno", true);
        ProjectGroup group = group(7L, Set.of(active));
        when(momentRepository.findByStatusAndGradingEnd(MomentStatus.LAUNCHED, TODAY.minusDays(1)))
                .thenReturn(List.of(moment));
        when(groupRepository.findByMomentIdOrderByName(4L)).thenReturn(List.of(group));
        when(reminderSentRepository.existsByKindAndReferenceIdAndRecipientId(ReminderKind.GRADES_PUBLISHED, 4L, 1L))
                .thenReturn(true);

        assertThat(service.enqueueDueReminders()).isZero();

        verifyNoInteractions(emailService);
    }

    @Test
    void withNothingDueNothingIsEnqueued() {
        assertThat(service.enqueueDueReminders()).isZero();

        verifyNoInteractions(emailService);
    }

    @Test
    void theDatesAreComputedFromTheInjectedClock() {
        service.enqueueDueReminders();

        verify(groupRepository).findBySubmissionDeadline(MomentStatus.LAUNCHED, LocalDate.of(2026, 11, 24));
        verify(groupRepository).findByMomentStatusAndMomentGradingEndOrderById(MomentStatus.LAUNCHED,
                LocalDate.of(2026, 11, 23));
        verify(momentRepository).findByStatusAndGradingEnd(MomentStatus.LAUNCHED, LocalDate.of(2026, 11, 21));
    }

    // ------------------------------------------------------------------------ ayudas

    private String sentBody(String to, String subject) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(to), eq(subject), body.capture());
        return body.getValue();
    }

    private ProjectGroup group(long id, Set<Student> members) {
        ProjectGroup group = mock(ProjectGroup.class);
        lenient().when(group.getId()).thenReturn(id);
        lenient().when(group.getName()).thenReturn("Grupo A");
        lenient().when(group.getMoment()).thenReturn(moment);
        lenient().when(group.getTeacher()).thenReturn(teacher);
        lenient().when(group.getMembers()).thenReturn(new LinkedHashSet<>(members));
        lenient().when(group.getEvaluators()).thenReturn(Set.of());
        return group;
    }

    private static Student student(long id, String email, String firstNames, boolean active) {
        Student student = mock(Student.class);
        lenient().when(student.getId()).thenReturn(id);
        lenient().when(student.isActive()).thenReturn(active);
        lenient().when(student.getEmail()).thenReturn(email);
        lenient().when(student.getFirstNames()).thenReturn(firstNames);
        return student;
    }

    private static ExternalEvaluator external(long id, String email, String firstNames, boolean active) {
        ExternalEvaluator evaluator = mock(ExternalEvaluator.class);
        lenient().when(evaluator.getId()).thenReturn(id);
        lenient().when(evaluator.isActive()).thenReturn(active);
        lenient().when(evaluator.getEmail()).thenReturn(email);
        lenient().when(evaluator.getFirstNames()).thenReturn(firstNames);
        return evaluator;
    }
}
