package com.fa26se088.filmlink.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String ERROR_CODE = "errorCode";

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), ex.getMessage());
    problem.setProperty(ERROR_CODE, errorCode.name());
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedException(Exception ex) {
    log.error("Unhandled exception", ex);
    ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), "Internal server error");
    problem.setProperty(ERROR_CODE, errorCode.name());
    return problem;
  }
}
