package CapstoneProjectFilmLink.exception;

/**
 * Thrown when a workflow precondition does not hold – e.g. trying to create a
 * SubTask whose deadline would exceed the parent Task's deadline. Mapped to
 * HTTP 400 by the standard exception handlers.
 */
public class WorkflowRuleViolationException extends RuntimeException {
    public WorkflowRuleViolationException(String message) {
        super(message);
    }
}
