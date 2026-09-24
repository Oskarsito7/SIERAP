/**
 * Presentation layer: REST controllers ({@code @RestController}).
 *
 * <p>They receive the HTTP request, validate the input with {@code @Valid},
 * delegate to a service and respond with DTOs. They contain neither business logic
 * nor {@code try/catch}: errors are translated by
 * {@code exception.GlobalExceptionHandler}.</p>
 */
package co.edu.unicesar.sierap.controller;
