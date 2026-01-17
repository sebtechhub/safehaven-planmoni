package org.planmoni.safehavenservice.exception;

public class SafeHavenNotFoundException extends RuntimeException {

    public SafeHavenNotFoundException(String message) {
        super(message);
    }

    public SafeHavenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
