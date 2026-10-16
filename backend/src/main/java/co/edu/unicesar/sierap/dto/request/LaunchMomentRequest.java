package co.edu.unicesar.sierap.dto.request;

/**
 * Petición para lanzar un momento a ejecución (ADR-012, ADR-018).
 *
 * @param confirmUnderMinimum {@code true} si el admin confirma el lanzamiento aun
 *                            cuando existan grupos por debajo del mínimo de integrantes
 */
public record LaunchMomentRequest(
        Boolean confirmUnderMinimum) {

    /**
     * Constructor por defecto: no confirma grupos por debajo del mínimo.
     */
    public LaunchMomentRequest() {
        this(false);
    }
}
