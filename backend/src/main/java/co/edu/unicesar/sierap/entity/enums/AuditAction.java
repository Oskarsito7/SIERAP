package co.edu.unicesar.sierap.entity.enums;

/**
 * Tipo de hecho que registra la bitácora de auditoría (ADR-023).
 *
 * <p>Se guarda como texto en {@code audit_log.action}; el frontend lo traduce al
 * español para mostrarlo.</p>
 */
public enum AuditAction {

    /** Se creó una cuenta (admin, docente, estudiante o evaluador externo). */
    ACCOUNT_CREATED,

    /** Se importaron estudiantes desde un CSV (un solo evento con la cantidad). */
    ACCOUNTS_IMPORTED,

    /** Se editaron datos de una cuenta. */
    ACCOUNT_UPDATED,

    /** Se activó una cuenta. */
    ACCOUNT_ACTIVATED,

    /** Se desactivó una cuenta. */
    ACCOUNT_DEACTIVATED,

    /** Se cambió el semestre de varios estudiantes a la vez. */
    SEMESTER_BULK_UPDATED,

    /** Se desactivó un código de registro. */
    REGISTRATION_CODE_DEACTIVATED,

    /** Se lanzó un momento. */
    MOMENT_LAUNCHED,

    /** Se prorrogaron las fechas de un momento ya lanzado. */
    MOMENT_EXTENDED,

    /** Se creó un parámetro global. */
    GLOBAL_PARAMETER_CREATED,

    /** Se cambió el valor de un parámetro global. */
    GLOBAL_PARAMETER_CHANGED,

    /** Se corrigió una evaluación que ya estaba enviada. */
    EVALUATION_CORRECTED,

    /** Se eliminó un grupo. */
    GROUP_DELETED,

    /** Se retiró a un integrante de un grupo. */
    GROUP_MEMBER_REMOVED,

    /** Se asignó un evaluador externo a un grupo. */
    GROUP_EVALUATOR_ASSIGNED,

    /** Se retiró un evaluador externo de un grupo. */
    GROUP_EVALUATOR_REMOVED
}
