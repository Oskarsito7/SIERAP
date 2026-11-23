package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de {@link NotificationServiceImpl}: destinatarios, asunto y datos
 * clave de cada correo, cuentas inactivas y encolado inmediato.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String FRONTEND = "http://localhost:5173";

    @Mock
    private EmailService emailService;

    private NotificationServiceImpl service;
    private ProjectGroup group;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties("una-clave-de-pruebas-de-al-menos-32-bytes", 15, 7, false,
                72, 1, 5, FRONTEND, "unicesar.edu.co", "SIERAP <no-reply@unicesar.edu.co>");
        service = new NotificationServiceImpl(emailService, properties);

        Program program = mock(Program.class);
        lenient().when(program.getName()).thenReturn("Ingeniería de Sistemas");
        Moment moment = mock(Moment.class);
        lenient().when(moment.getName()).thenReturn("Momento 1");
        lenient().when(moment.getProgram()).thenReturn(program);
        Teacher teacher = mock(Teacher.class);
        lenient().when(teacher.getFullName()).thenReturn("Ana Docente");
        group = mock(ProjectGroup.class);
        lenient().when(group.getId()).thenReturn(7L);
        lenient().when(group.getName()).thenReturn("Grupo A");
        lenient().when(group.getProjectTitle()).thenReturn("Sistema de riego");
        lenient().when(group.getMoment()).thenReturn(moment);
        lenient().when(group.getTeacher()).thenReturn(teacher);
        lenient().when(group.getEvaluationDate()).thenReturn(LocalDate.of(2026, 12, 3));
    }

    @Test
    void assignedEvaluatorGetsTheProjectDetailsAndTheLink() {
        service.notifyEvaluatorAssigned(evaluator(true), group);

        String body = sentBody("empresario@correo.com", "SIERAP: te asignaron un proyecto para evaluar");
        assertThat(body).contains("Hola, Eva:", "Ingeniería de Sistemas", "Momento 1", "Grupo A",
                "Sistema de riego", "Ana Docente", "03/12/2026",
                FRONTEND + "/assigned-groups/7", "SIERAP · Universidad Popular del Cesar");
    }

    @Test
    void assignedEvaluatorEmailShowsPlaceholdersWhenThereIsNoTitleOrDate() {
        lenient().when(group.getProjectTitle()).thenReturn(null);
        lenient().when(group.getEvaluationDate()).thenReturn(null);

        service.notifyEvaluatorAssigned(evaluator(true), group);

        String body = sentBody("empresario@correo.com", "SIERAP: te asignaron un proyecto para evaluar");
        assertThat(body).contains("Proyecto: sin título", "Fecha de evaluación: por fijar");
    }

    @Test
    void inactiveEvaluatorGetsNothing() {
        service.notifyEvaluatorAssigned(evaluator(false), group);

        verifyNoInteractions(emailService);
    }

    @Test
    void uploadSendsTheReceiptToEveryActiveMemberIncludingTheUploader() {
        Student uploader = student("uno@unicesar.edu.co", "Uno", true);
        Student mate = student("dos@unicesar.edu.co", "Dos", true);
        Student inactive = student("tres@unicesar.edu.co", "Tres", false);
        lenient().when(uploader.getFullName()).thenReturn("Uno Estudiante");
        lenient().when(group.getMembers()).thenReturn(Set.of(uploader, mate, inactive));
        Submission submission = submission(true);

        service.notifySubmissionUploaded(group, submission, uploader);

        String subject = "SIERAP: tu grupo subió la versión 3";
        String body = sentBody("uno@unicesar.edu.co", subject);
        sentBody("dos@unicesar.edu.co", subject);
        verify(emailService, never()).send(org.mockito.ArgumentMatchers.eq("tres@unicesar.edu.co"), anyString(),
                anyString());
        assertThat(body).contains("Grupo A", "Momento 1", "Versión: 3", "Subida por: Uno Estudiante",
                "22/11/2026 14:35", "Entrega tardía: sí", "entrega.zip (2,5 MB)", "slides.pptx (300 KB)",
                FRONTEND + "/my-group?groupId=7");
    }

    @Test
    void uploadWithoutPresentationOmitsItAndMarksOnTime() {
        Student uploader = student("uno@unicesar.edu.co", "Uno", true);
        lenient().when(uploader.getFullName()).thenReturn("Uno Estudiante");
        lenient().when(group.getMembers()).thenReturn(Set.of(uploader));

        service.notifySubmissionUploaded(group, submission(false), uploader);

        String body = sentBody("uno@unicesar.edu.co", "SIERAP: tu grupo subió la versión 3");
        assertThat(body).contains("Entrega tardía: no").doesNotContain("Presentación:");
    }

    @Test
    void addedStudentGetsTheGroupDetails() {
        service.notifyMemberAdded(student("uno@unicesar.edu.co", "Uno", true), group);

        String body = sentBody("uno@unicesar.edu.co", "SIERAP: te agregaron a un grupo");
        assertThat(body).contains("Hola, Uno:", "Grupo A", "Sistema de riego", "Momento 1", "Ana Docente",
                FRONTEND + "/my-group?groupId=7");
    }

    @Test
    void inactiveStudentAddedGetsNothing() {
        service.notifyMemberAdded(student("uno@unicesar.edu.co", "Uno", false), group);

        verifyNoInteractions(emailService);
    }

    @Test
    void theEmailIsEnqueuedRightAwayWithoutWaitingForACommit() {
        service.notifyMemberAdded(student("uno@unicesar.edu.co", "Uno", true), group);

        // Encolar es insertar en la cola dentro de la transacción de la operación (ADR-022):
        // si esta se deshace, la fila se deshace con ella, así que no hay que esperar al commit.
        verify(emailService, times(1)).send(org.mockito.ArgumentMatchers.eq("uno@unicesar.edu.co"), anyString(),
                anyString());
    }

    private String sentBody(String to, String subject) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(org.mockito.ArgumentMatchers.eq(to),
                org.mockito.ArgumentMatchers.eq(subject), body.capture());
        return body.getValue();
    }

    private static ExternalEvaluator evaluator(boolean active) {
        ExternalEvaluator evaluator = mock(ExternalEvaluator.class);
        lenient().when(evaluator.isActive()).thenReturn(active);
        lenient().when(evaluator.getEmail()).thenReturn("empresario@correo.com");
        lenient().when(evaluator.getFirstNames()).thenReturn("Eva");
        return evaluator;
    }

    private static Student student(String email, String firstNames, boolean active) {
        Student student = mock(Student.class);
        lenient().when(student.isActive()).thenReturn(active);
        lenient().when(student.getEmail()).thenReturn(email);
        lenient().when(student.getFirstNames()).thenReturn(firstNames);
        return student;
    }

    private static Submission submission(boolean full) {
        Submission submission = mock(Submission.class);
        lenient().when(submission.getVersionNumber()).thenReturn(3);
        lenient().when(submission.getSubmittedAt()).thenReturn(LocalDateTime.of(2026, 11, 22, 14, 35, 10));
        lenient().when(submission.isLate()).thenReturn(full);
        lenient().when(submission.getZipFileName()).thenReturn("entrega.zip");
        lenient().when(submission.getZipSize()).thenReturn(2_621_440L);
        lenient().when(submission.hasPresentation()).thenReturn(full);
        lenient().when(submission.getPresentationFileName()).thenReturn("slides.pptx");
        lenient().when(submission.getPresentationSize()).thenReturn(307_200L);
        return submission;
    }
}
