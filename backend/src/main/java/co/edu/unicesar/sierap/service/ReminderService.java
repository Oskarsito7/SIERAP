package co.edu.unicesar.sierap.service;

/**
 * Recordatorios por correo disparados por fechas (ADR-022).
 *
 * <p>Se ejecuta una vez al día. Cada recordatorio se encola en la cola de correos y deja
 * una constancia ({@code reminder_sent}), de modo que correr el trabajo dos veces el mismo
 * día, o reiniciar el servidor, no repite los avisos.</p>
 */
public interface ReminderService {

    /**
     * Encola los recordatorios que corresponden a hoy:
     * <ul>
     *   <li>cierre de entregas en dos días, a los integrantes activos de los grupos que aún
     *       no tienen ninguna entrega;</li>
     *   <li>evaluación pendiente, un día antes del fin de la calificación, al docente o
     *       evaluador externo que aún no envía su evaluación;</li>
     *   <li>notas publicadas, el día siguiente al fin de la calificación, a los estudiantes
     *       activos con grupo en el momento.</li>
     * </ul>
     * Solo considera momentos lanzados.
     *
     * @return cantidad de correos encolados
     */
    int enqueueDueReminders();
}
