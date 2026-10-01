package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un archivo subido supera el tamaño máximo de su tipo, p. ej. una
 * presentación de más de 30 MB (ADR-015). La API responde con
 * {@code 413 Content Too Large}.
 *
 * <p>Las cargas que superan el límite de Spring
 * ({@code spring.servlet.multipart.max-file-size}) no llegan al service: Spring las
 * rechaza antes con el mismo código.</p>
 */
public class FileTooLargeException extends SierapException {

    /**
     * Crea la excepción con el límite que se superó.
     *
     * @param message qué archivo es demasiado grande y cuál es su máximo
     */
    public FileTooLargeException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.PAYLOAD_TOO_LARGE;
    }
}
