/**
 * Data transfer objects (DTOs) at the API boundary.
 *
 * <p>JPA entities never leave the API: every endpoint receives a DTO from
 * {@code dto.request} and responds with one from {@code dto.response}. They are
 * modeled as Java {@code record}s (immutable).</p>
 */
package co.edu.unicesar.sierap.dto;
