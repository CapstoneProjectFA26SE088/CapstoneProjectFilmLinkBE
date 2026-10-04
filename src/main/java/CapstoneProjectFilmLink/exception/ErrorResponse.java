package CapstoneProjectFilmLink.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private String path;
    private String errorCode;
    private Map<String, Object> details;

    public ErrorResponse(int status, String message, String error) {
        this.status = status;
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    // Manual setter for path (in case Lombok doesn't work properly)
    public void setPath(String path) {
        this.path = path;
    }

    // Manual getter for path
    public String getPath() {
        return path;
    }
}
