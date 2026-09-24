package co.edu.unicesar.sierap.entity.enums;

/**
 * Lifecycle status of a {@code Moment} (ADR-012).
 *
 * <p>It is a system-flow value, not an assessment business value: what can be done
 * in each status also depends on the date windows of the moment.</p>
 */
public enum MomentStatus {

    /**
     * The admin configures learning outcomes, criteria, levels, members and
     * windows; inside the group formation window students join the groups.
     */
    CONFIGURATION,

    /**
     * The admin launched the moment: configuration and groups are locked and the
     * date windows mark when work is submitted and when it is graded.
     */
    LAUNCHED
}
