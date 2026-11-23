package co.edu.unicesar.sierap.entity.enums;

/**
 * Estado de un correo en la cola de salida (ADR-022).
 */
public enum EmailStatus {

    /** Esperando su primer envío o un reintento. */
    PENDING,

    /** Enviado al servidor SMTP. */
    SENT,

    /** Agotó los intentos; ya no se reintenta. */
    FAILED
}
