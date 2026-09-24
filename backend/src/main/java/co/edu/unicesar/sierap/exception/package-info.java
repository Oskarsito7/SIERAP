/**
 * Centralized error handling.
 *
 * <p>{@link co.edu.unicesar.sierap.exception.SierapException} is the base of the
 * business errors; each subclass defines its HTTP status.
 * {@link co.edu.unicesar.sierap.exception.GlobalExceptionHandler} translates them
 * into a uniform response.</p>
 */
package co.edu.unicesar.sierap.exception;
