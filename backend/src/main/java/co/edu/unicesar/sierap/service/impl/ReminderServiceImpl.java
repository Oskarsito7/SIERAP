package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.ReminderSent;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.entity.enums.ReminderKind;
import co.edu.unicesar.sierap.repository.EvaluationRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.ReminderSentRepository;
import co.edu.unicesar.sierap.repository.SubmissionRepository;
import co.edu.unicesar.sierap.service.EmailService;
import co.edu.unicesar.sierap.service.ReminderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ReminderService}.
 *
 * <p>Todo ocurre en una sola transacción: si algo falla, no queda ningún correo encolado
 * ni constancia a medias y el trabajo del día siguiente (o un reintento manual) lo
 * retoma. "Hoy" sale siempre del {@link Clock} inyectado.</p>
 */
@Service
@Transactional
public class ReminderServiceImpl implements ReminderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIGNATURE = "SIERAP · Universidad Popular del Cesar";
    /** Días de antelación del aviso de cierre de entregas. */
    private static final int SUBMISSION_REMINDER_DAYS = 2;
    /** Marca del cuerpo donde se inserta el nombre del destinatario. */
    private static final String NAME_PLACEHOLDER = "{name}";

    private final ProjectGroupRepository groupRepository;
    private final MomentRepository momentRepository;
    private final SubmissionRepository submissionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ReminderSentRepository reminderSentRepository;
    private final EmailService emailService;
    private final AuthProperties properties;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param groupRepository        repositorio de grupos
     * @param momentRepository       repositorio de momentos
     * @param submissionRepository   repositorio de entregas
     * @param evaluationRepository   repositorio de evaluaciones
     * @param reminderSentRepository constancias de recordatorios ya encolados
     * @param emailService           cola de correos
     * @param properties             parámetros de autenticación (URL del frontend)
     * @param clock                  reloj de la aplicación
     */
    public ReminderServiceImpl(ProjectGroupRepository groupRepository, MomentRepository momentRepository,
                               SubmissionRepository submissionRepository,
                               EvaluationRepository evaluationRepository,
                               ReminderSentRepository reminderSentRepository, EmailService emailService,
                               AuthProperties properties, Clock clock) {
        this.groupRepository = groupRepository;
        this.momentRepository = momentRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationRepository = evaluationRepository;
        this.reminderSentRepository = reminderSentRepository;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public int enqueueDueReminders() {
        LocalDate today = LocalDate.now(clock);
        int enqueued = 0;
        enqueued += remindSubmissionClosing(today);
        enqueued += remindPendingEvaluations(today);
        enqueued += remindPublishedGrades(today);
        return enqueued;
    }

    /** Cierre de entregas en dos días, solo para grupos sin ninguna entrega. */
    private int remindSubmissionClosing(LocalDate today) {
        LocalDate deadline = today.plusDays(SUBMISSION_REMINDER_DAYS);
        int enqueued = 0;
        for (ProjectGroup group : groupRepository.findBySubmissionDeadline(MomentStatus.LAUNCHED, deadline)) {
            if (submissionRepository.existsByProjectGroupId(group.getId())) {
                continue;
            }
            Moment moment = group.getMoment();
            String body = "Hola, {name}:\n\n"
                    + "El plazo para entregar el proyecto de tu grupo en SIERAP vence en dos días, el "
                    + deadline.format(DATE) + ", y tu grupo todavía no ha subido ninguna entrega.\n\n"
                    + "Grupo: " + group.getName() + "\n"
                    + "Momento: " + moment.getName() + "\n\n"
                    + "Puedes subir la entrega en este enlace:\n"
                    + properties.frontendUrl() + "/my-group?groupId=" + group.getId() + "\n\n"
                    + SIGNATURE;
            for (User member : group.getMembers()) {
                if (member.isActive() && enqueueOnce(ReminderKind.SUBMISSION_CLOSING, group.getId(), member,
                        "SIERAP: la entrega de tu grupo vence en dos días", body)) {
                    enqueued++;
                }
            }
        }
        return enqueued;
    }

    /** Evaluación pendiente un día antes del fin de la calificación. */
    private int remindPendingEvaluations(LocalDate today) {
        LocalDate gradingEnd = today.plusDays(1);
        int enqueued = 0;
        for (ProjectGroup group : groupRepository.findByMomentStatusAndMomentGradingEndOrderById(
                MomentStatus.LAUNCHED, gradingEnd)) {
            Moment moment = group.getMoment();
            List<User> evaluators = new ArrayList<>();
            evaluators.add(group.getTeacher());
            evaluators.addAll(group.getEvaluators());
            for (User evaluator : evaluators) {
                if (!evaluator.isActive() || evaluationRepository.existsByProjectGroupIdAndEvaluatorIdAndStatus(
                        group.getId(), evaluator.getId(), EvaluationStatus.SUBMITTED)) {
                    continue;
                }
                String path = evaluator instanceof ExternalEvaluator
                        ? "/assigned-groups/" + group.getId()
                        : "/groups/" + group.getId() + "/evaluation";
                String body = "Hola, {name}:\n\n"
                        + "Mañana, " + gradingEnd.format(DATE) + ", termina el plazo para calificar y todavía "
                        + "no has enviado tu evaluación de este grupo.\n\n"
                        + "Grupo: " + group.getName() + "\n"
                        + "Momento: " + moment.getName() + "\n\n"
                        + "Puedes calificarlo en este enlace:\n"
                        + properties.frontendUrl() + path + "\n\n"
                        + SIGNATURE;
                if (enqueueOnce(ReminderKind.EVALUATION_PENDING, group.getId(), evaluator,
                        "SIERAP: tienes una evaluación pendiente", body)) {
                    enqueued++;
                }
            }
        }
        return enqueued;
    }

    /** Notas publicadas el día siguiente al fin de la calificación. */
    private int remindPublishedGrades(LocalDate today) {
        int enqueued = 0;
        for (Moment moment : momentRepository.findByStatusAndGradingEnd(MomentStatus.LAUNCHED,
                today.minusDays(1))) {
            String body = "Hola, {name}:\n\n"
                    + "Terminó la calificación del " + moment.getName() + " y ya puedes ver tus notas "
                    + "por RAP en SIERAP:\n"
                    + properties.frontendUrl() + "/my-grades?momentId=" + moment.getId() + "\n\n"
                    + SIGNATURE;
            for (ProjectGroup group : groupRepository.findByMomentIdOrderByName(moment.getId())) {
                for (User student : group.getMembers()) {
                    if (student.isActive() && enqueueOnce(ReminderKind.GRADES_PUBLISHED, moment.getId(), student,
                            "SIERAP: ya puedes ver tus notas", body)) {
                        enqueued++;
                    }
                }
            }
        }
        return enqueued;
    }

    /**
     * Encola el recordatorio si no se había encolado ya para ese destinatario y deja la
     * constancia. El cuerpo lleva {@code {name}} donde va el nombre del destinatario.
     *
     * @return {@code true} si se encoló, {@code false} si ya estaba hecho
     */
    private boolean enqueueOnce(ReminderKind kind, Long referenceId, User recipient, String subject,
                                String bodyTemplate) {
        if (reminderSentRepository.existsByKindAndReferenceIdAndRecipientId(kind, referenceId,
                recipient.getId())) {
            return false;
        }
        String body = bodyTemplate.replace(NAME_PLACEHOLDER, recipient.getFirstNames());
        emailService.send(recipient.getEmail(), subject, body);
        reminderSentRepository.save(new ReminderSent(kind, referenceId, recipient.getId(), LocalDate.now(clock)));
        return true;
    }
}
