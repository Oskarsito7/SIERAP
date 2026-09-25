package co.edu.unicesar.sierap.entity.enums;

/**
 * Rol de un usuario de SIERAP.
 *
 * <p>Es también el discriminador de la herencia {@code JOINED} de {@code User}
 * (columna {@code app_user.role}, ADR-009) y la base del control de acceso con
 * Spring Security. Los tipos de evaluador externo (empresario, egresado…) no son
 * roles sino valores del catálogo {@code EvaluatorType} (ADR-011).</p>
 */
public enum UserRole {

    /** Administra la institución: parámetros, periodos, facultades y tipos de evaluador. */
    SUPER_ADMIN,

    /** Administra una facultad: crea, configura y lanza los momentos de sus programas. */
    ADMIN,

    /** Docente: habilita grupos, designa evaluadores externos y califica sus grupos. */
    TEACHER,

    /** Estudiante: se inscribe en un grupo por momento, entrega y consulta su nota. */
    STUDENT,

    /** Evaluador que no es docente (empresario, egresado u otro tipo del catálogo). */
    EXTERNAL_EVALUATOR
}
