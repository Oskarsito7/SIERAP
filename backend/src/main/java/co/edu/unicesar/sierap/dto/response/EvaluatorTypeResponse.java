package co.edu.unicesar.sierap.dto.response;

/**
 * Tipo de evaluador externo del catálogo.
 *
 * @param id          identificador
 * @param name        nombre visible
 * @param description descripción
 * @param active         {@code true} si se puede asignar a evaluadores nuevos
 * @param evaluatorCount cantidad de evaluadores externos con este tipo
 */
public record EvaluatorTypeResponse(
        Long id,
        String name,
        String description,
        boolean active,
        int evaluatorCount) {
}
