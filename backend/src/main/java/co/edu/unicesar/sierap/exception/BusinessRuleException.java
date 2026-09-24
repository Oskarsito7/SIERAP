package co.edu.unicesar.sierap.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation breaks a business rule, e.g. a group with more members
 * than allowed or a submission after the deadline. The API responds with
 * {@code 422 Unprocessable Entity}.
 */
public class BusinessRuleException extends SierapException {

    /**
     * Creates the exception with the description of the broken rule.
     *
     * @param message why the operation is not allowed, for the end user
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
