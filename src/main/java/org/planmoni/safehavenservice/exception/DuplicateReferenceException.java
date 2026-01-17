package org.planmoni.safehavenservice.exception;

public class DuplicateReferenceException extends RuntimeException {

    public DuplicateReferenceException(String message) {
        super(message);
    }

    public DuplicateReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
