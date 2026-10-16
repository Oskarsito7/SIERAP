/**
 * Objetos de transferencia de datos (DTO) del borde de la API.
 *
 * <p>Las entidades JPA nunca salen de la API: cada endpoint recibe un DTO de
 * {@code dto.request} y responde con uno de {@code dto.response}. Se modelan como
 * {@code record} de Java (inmutables).</p>
 */
package co.edu.unicesar.sierap.dto;
