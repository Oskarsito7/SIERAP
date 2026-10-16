package co.edu.unicesar.sierap.entity.enums;

/**
 * Estado de una {@code Evaluation}.
 *
 * <p>Como {@link MomentStatus}, es un valor del flujo del sistema y no un valor de
 * negocio de la evaluación.</p>
 */
public enum EvaluationStatus {

    /** Guardada a medias: todavía no cuenta para la nota del grupo. */
    DRAFT,

    /**
     * Enviada: cuenta para la nota del grupo. Se puede corregir mientras la ventana
     * de calificación esté abierta.
     */
    SUBMITTED
}
