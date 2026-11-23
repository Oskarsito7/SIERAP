package co.edu.unicesar.sierap.entity.enums;

/**
 * Tipo de recordatorio programado por fecha (ADR-022).
 */
public enum ReminderKind {

    /** Faltan dos días para el cierre de entregas y el grupo no ha entregado. */
    SUBMISSION_CLOSING,

    /** Mañana termina la calificación y el evaluador no ha enviado su evaluación. */
    EVALUATION_PENDING,

    /** Terminó la calificación: los estudiantes ya pueden ver sus notas. */
    GRADES_PUBLISHED
}
