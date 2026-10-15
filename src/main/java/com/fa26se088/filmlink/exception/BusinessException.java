package com.fa26se088.filmlink.exception;

import java.util.Map;
import lombok.Getter;

/**
 * Lỗi nghiệp vụ ném từ tầng service. {@link GlobalExceptionHandler} chuyển thành {@code
 * ProblemDetail} theo {@link ErrorCode}.
 *
 * <p>{@code properties} là dữ liệu bổ sung FE cần để hiển thị (ví dụ {@code lockReason} khi tài
 * khoản bị khoá). Không đưa dữ liệu nhạy cảm vào đây.
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> properties;

  public BusinessException(ErrorCode errorCode, String message) {
    this(errorCode, message, Map.of());
  }

  public BusinessException(ErrorCode errorCode, String message, Map<String, Object> properties) {
    super(message);
    this.errorCode = errorCode;
    this.properties = Map.copyOf(properties);
  }
}
