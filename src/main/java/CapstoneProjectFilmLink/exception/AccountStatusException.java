package CapstoneProjectFilmLink.exception;

public class AccountStatusException extends RuntimeException {
    private final String errorCode;
    private final String rejectionReason;

    public AccountStatusException(String errorCode, String message, String rejectionReason) {
        super(message);
        this.errorCode = errorCode;
        this.rejectionReason = rejectionReason;
    }

    public String getErrorCode() { return errorCode; }
    public String getRejectionReason() { return rejectionReason; }
}
