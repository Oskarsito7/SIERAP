/**
 * Manejo centralizado de errores.
 *
 * <p>{@link co.edu.unicesar.sierap.exception.SierapException} es la base de los
 * errores de negocio; cada subclase define su código HTTP.
 * {@link co.edu.unicesar.sierap.exception.GlobalExceptionHandler} los traduce a
 * una respuesta uniforme.</p>
 */
package co.edu.unicesar.sierap.exception;
