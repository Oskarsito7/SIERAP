package co.edu.unicesar.sierap.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.service.EmailService;
import co.edu.unicesar.sierap.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementación de {@link NotificationService}.
 *
 * <p>Los datos del correo se leen de las entidades mientras la transacción sigue abierta
 * y se guardan en valores simples, de modo que el envío posterior al commit no toca
 * entidades perezosas. El texto es plano y en español, firmado por SIERAP.</p>
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIGNATURE = "SIERAP · Universidad Popular del Cesar";

    private final EmailService emailService;
    private final AuthProperties properties;

    /**
     * Crea el servicio.
     *
     * @param emailService servicio de envío de correos
     * @param properties   parámetros de autenticación (URL del frontend para los enlaces)
     */
    public NotificationServiceImpl(EmailService emailService, AuthProperties properties) {
        this.emailService = emailService;
        this.properties = properties;
    }

    @Override
    public void notifyEvaluatorAssigned(ExternalEvaluator evaluator, ProjectGroup group) {
        if (!evaluator.isActive()) {
            return;
        }
        Moment moment = group.getMoment();
        String body = "Hola, " + evaluator.getFirstNames() + ":\n\n"
                + "Se te asignó un proyecto para evaluar en SIERAP.\n\n"
                + "Programa: " + moment.getProgram().getName() + "\n"
                + "Momento: " + moment.getName() + "\n"
                + "Grupo: " + group.getName() + "\n"
                + "Proyecto: " + titleOf(group) + "\n"
                + "Docente: " + group.getTeacher().getFullName() + "\n"
                + "Fecha de evaluación: " + dateOrPending(group.getEvaluationDate()) + "\n\n"
                + "Puedes ver el grupo y calificarlo en este enlace:\n"
                + properties.frontendUrl() + "/assigned-groups/" + group.getId() + "\n\n"
                + SIGNATURE;
        sendAfterCommit(evaluator.getEmail(), "SIERAP: te asignaron un proyecto para evaluar", body);
    }

    @Override
    public void notifySubmissionUploaded(ProjectGroup group, Submission submission, Student uploader) {
        List<String> recipients = group.getMembers().stream()
                .filter(User::isActive)
                .map(User::getEmail)
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        StringBuilder body = new StringBuilder("Hola:\n\n")
                .append("Tu grupo subió una nueva versión de la entrega en SIERAP. ")
                .append("Este correo sirve de comprobante.\n\n")
                .append("Grupo: ").append(group.getName()).append('\n')
                .append("Momento: ").append(group.getMoment().getName()).append('\n')
                .append("Versión: ").append(submission.getVersionNumber()).append('\n')
                .append("Subida por: ").append(uploader.getFullName()).append('\n')
                .append("Fecha y hora: ").append(submission.getSubmittedAt().format(DATE_TIME)).append('\n')
                .append("Entrega tardía: ").append(submission.isLate() ? "sí" : "no").append('\n')
                .append("Archivo .zip: ").append(submission.getZipFileName())
                .append(" (").append(formatSize(submission.getZipSize())).append(")\n");
        if (submission.hasPresentation()) {
            body.append("Presentación: ").append(submission.getPresentationFileName())
                    .append(" (").append(formatSize(submission.getPresentationSize())).append(")\n");
        }
        body.append("\nPuedes ver el historial de entregas en este enlace:\n")
                .append(properties.frontendUrl()).append("/my-group?groupId=").append(group.getId())
                .append("\n\n").append(SIGNATURE);
        String subject = "SIERAP: tu grupo subió la versión " + submission.getVersionNumber();
        String text = body.toString();
        recipients.forEach(recipient -> sendAfterCommit(recipient, subject, text));
    }

    @Override
    public void notifyMemberAdded(Student student, ProjectGroup group) {
        if (!student.isActive()) {
            return;
        }
        String body = "Hola, " + student.getFirstNames() + ":\n\n"
                + "Tu docente te agregó a un grupo en SIERAP.\n\n"
                + "Grupo: " + group.getName() + "\n"
                + "Proyecto: " + titleOf(group) + "\n"
                + "Momento: " + group.getMoment().getName() + "\n"
                + "Docente: " + group.getTeacher().getFullName() + "\n\n"
                + "Puedes ver tu grupo en este enlace:\n"
                + properties.frontendUrl() + "/my-group?groupId=" + group.getId() + "\n\n"
                + SIGNATURE;
        sendAfterCommit(student.getEmail(), "SIERAP: te agregaron a un grupo", body);
    }

    private static String titleOf(ProjectGroup group) {
        String title = group.getProjectTitle();
        return title == null || title.isBlank() ? "sin título" : title;
    }

    private static String dateOrPending(LocalDate date) {
        return date == null ? "por fijar" : date.format(DATE);
    }

    private static String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return Math.max(1, Math.round(bytes / 1024.0)) + " KB";
    }

    /**
     * Envía cuando la transacción actual hace commit, o de inmediato si no hay
     * transacción: así no sale un aviso de una operación que luego se deshace ni se
     * retiene la conexión a la base de datos mientras responde el servidor SMTP.
     */
    private void sendAfterCommit(String to, String subject, String body) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.send(to, subject, body);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.send(to, subject, body);
            }
        });
    }
}
