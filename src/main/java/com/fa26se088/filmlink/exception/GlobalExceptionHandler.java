package com.fa26se088.filmlink.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Chuyển mọi lỗi thành {@code ProblemDetail} có hai property {@code errorCode} và {@code
 * messageCode}. Không trả stack trace, tên bảng hay thông điệp kỹ thuật (CR-06).
 *
 * <p>Lỗi 401/403 phát sinh trong filter của Spring Security cũng được đưa về đây (xem {@code
 * security.ProblemDetailAuthenticationEntryPoint}), nên toàn hệ thống chỉ có một định dạng lỗi.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String ERROR_CODE = "errorCode";
  private static final String MESSAGE_CODE = "messageCode";

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusinessException(BusinessException ex) {
    ProblemDetail problem = toProblem(ex.getErrorCode(), ex.getMessage());
    ex.getProperties().forEach(problem::setProperty);
    return problem;
  }

  /** Chưa đăng nhập hoặc access token không hợp lệ. */
  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
    return toProblem(ErrorCode.AUTHENTICATION_REQUIRED, "Cần đăng nhập để truy cập tài nguyên này.");
  }

  /** Đã đăng nhập nhưng không đủ quyền, gồm cả lỗi từ {@code @PreAuthorize}. */
  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
    return toProblem(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện thao tác này.");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedException(Exception ex) {
    log.error("Unhandled exception", ex);
    return toProblem(ErrorCode.INTERNAL_ERROR, "Internal server error");
  }

  /** Lỗi Bean Validation của request body ({@code @Valid}): trả danh sách {field, message}. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem = toProblem(ErrorCode.VALIDATION_ERROR, "Dữ liệu gửi lên không hợp lệ.");
    List<FieldViolation> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  /**
   * Các lỗi Spring MVC còn lại (JSON sai cú pháp, sai method, sai content type…) do lớp cha tạo
   * {@code ProblemDetail}; ở đây chỉ bổ sung {@code errorCode} để định dạng lỗi luôn đồng nhất.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, status, request);
    if (response != null
        && response.getBody() instanceof ProblemDetail problem
        && (problem.getProperties() == null || !problem.getProperties().containsKey(ERROR_CODE))) {
      ErrorCode errorCode = fallbackErrorCode(status);
      problem.setProperty(ERROR_CODE, errorCode.name());
      problem.setProperty(MESSAGE_CODE, errorCode.getMessageCode());
    }
    return response;
  }

  private static ErrorCode fallbackErrorCode(HttpStatusCode status) {
    if (status.value() == HttpStatus.BAD_REQUEST.value()) {
      return ErrorCode.VALIDATION_ERROR;
    }
    if (status.value() == HttpStatus.NOT_FOUND.value()) {
      return ErrorCode.RESOURCE_NOT_FOUND;
    }
    return status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_NOT_SUPPORTED;
  }

  private static ProblemDetail toProblem(ErrorCode errorCode, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), detail);
    problem.setProperty(ERROR_CODE, errorCode.name());
    problem.setProperty(MESSAGE_CODE, errorCode.getMessageCode());
    return problem;
  }

  /** Một lỗi validation của một field trong request body. */
  public record FieldViolation(String field, String message) {}
}
