package com.fa26se088.filmlink.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
  BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus httpStatus;
}
