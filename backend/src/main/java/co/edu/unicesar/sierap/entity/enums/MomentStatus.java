package co.edu.unicesar.sierap.entity.enums;

/**
 * Estado del ciclo de vida de un {@code Moment} (ADR-012).
 *
 * <p>Es un valor del flujo del sistema, no un valor de negocio de la evaluación:
 * lo que se puede hacer dentro de cada estado lo determinan además las ventanas de
 * fechas del momento.</p>
 */
public enum MomentStatus {

    /**
     * El admin configura RAP, criterios, niveles, integrantes y ventanas; dentro de
     * la ventana de conformación los estudiantes se inscriben en los grupos.
     */
    CONFIGURATION,

    /**
     * El admin lanzó el momento: la configuración y los grupos quedan bloqueados y
     * las ventanas de fechas marcan cuándo se entrega y cuándo se califica.
     */
    LAUNCHED
}
