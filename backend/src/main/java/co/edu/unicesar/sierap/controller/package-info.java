/**
 * Capa de presentación: controllers REST ({@code @RestController}).
 *
 * <p>Reciben la petición HTTP, validan la entrada con {@code @Valid}, delegan en un
 * service y responden con DTOs. No contienen lógica de negocio ni {@code try/catch}:
 * los errores los traduce {@code exception.GlobalExceptionHandler}.</p>
 */
package co.edu.unicesar.sierap.controller;
