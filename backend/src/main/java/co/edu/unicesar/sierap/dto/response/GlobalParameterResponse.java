package co.edu.unicesar.sierap.dto.response;

/**
 * Parámetro global del sistema.
 *
 * @param key         clave única del parámetro
 * @param value       valor configurado
 * @param description descripción del propósito del parámetro
 */
public record GlobalParameterResponse(
        String key,
        String value,
        String description) {
}
