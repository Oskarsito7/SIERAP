package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;

/**
 * Notificaciones por correo de los eventos del flujo de un momento.
 *
 * <p>Los correos se encolan en la cola de correos (ADR-022) dentro de la transacción que
 * los pide: si la operación se deshace, no sale ninguno. Solo se avisa a cuentas activas.
 * El envío real lo hace después el despachador, con reintentos; un fallo del servidor de
 * correo nunca deshace la operación.</p>
 */
public interface NotificationService {

    /**
     * Avisa al evaluador externo que se le asignó un grupo para evaluar.
     *
     * @param evaluator evaluador designado
     * @param group     grupo asignado
     */
    void notifyEvaluatorAssigned(ExternalEvaluator evaluator, ProjectGroup group);

    /**
     * Avisa a los integrantes activos del grupo que se subió una versión de la entrega;
     * sirve de comprobante.
     *
     * @param group      grupo que entregó
     * @param submission versión subida
     * @param uploader   estudiante que la subió
     */
    void notifySubmissionUploaded(ProjectGroup group, Submission submission, Student uploader);

    /**
     * Avisa al estudiante que su docente lo agregó a un grupo.
     *
     * @param student estudiante agregado
     * @param group   grupo al que se agregó
     */
    void notifyMemberAdded(Student student, ProjectGroup group);
}
