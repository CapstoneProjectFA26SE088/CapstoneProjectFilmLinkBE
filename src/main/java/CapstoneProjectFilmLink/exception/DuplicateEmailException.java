package CapstoneProjectFilmLink.exception;

/**
 * Exception thrown when trying to create a user with an email that already exists
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}

