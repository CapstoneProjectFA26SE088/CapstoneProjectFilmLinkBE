package com.fa26se088.filmlink.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi trả cho client trong property {@code errorCode} của {@code ProblemDetail}.
 *
 * <p>Đặt tên theo mẫu {@code <DOMAIN>_<LÝ_DO>}. {@code messageCode} là mã thông báo hiển thị của
 * SRS (Appendix 5.3, MSG01–MSG37): FE dùng mã này để chọn câu tiếng Việt (CR-13). Lỗi không có
 * thông báo tương ứng trong SRS thì {@code messageCode = null}.
 */
@Getter
public enum ErrorCode {
  // --- Chung ---
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, null),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, null),
  REQUEST_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, null),
  BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, null),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, null),

  // --- Xác thực / phân quyền ---
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, null),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, null),

  // --- account ---
  ACCOUNT_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "MSG09"),
  ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, null),
  ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, null),
  ACCOUNT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, null),
  ACCOUNT_OTP_INVALID(HttpStatus.UNPROCESSABLE_CONTENT, "MSG11"),
  ACCOUNT_REGISTRATION_REJECTED(HttpStatus.UNPROCESSABLE_CONTENT, null),

  // --- lab ---
  LAB_NOT_FOUND(HttpStatus.NOT_FOUND, null),
  LAB_ACCESS_DENIED(HttpStatus.FORBIDDEN, null),

  // --- payment ---
  PAYMENT_GATEWAY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, null),

  // --- storage (client/storage) ---
  STORAGE_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, null),
  STORAGE_FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, null),
  /** Sai chữ ký và hết hạn dùng chung một mã, không tiết lộ lý do. */
  STORAGE_LINK_INVALID(HttpStatus.FORBIDDEN, null),
  /** UH-39: storage lỗi thì báo rõ, phần khác vẫn chạy. */
  STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, null),

  // --- masterdata (MD-02, UC54) ---
  /** Mã code đã tồn tại trong bảng danh mục (BR-16). */
  MASTERDATA_CODE_DUPLICATE(HttpStatus.CONFLICT, null),
  /** Danh mục đang được tham chiếu, không thể xóa cứng; phải chuyển sang DISCONTINUED (BR-16). */
  MASTERDATA_IN_USE(HttpStatus.UNPROCESSABLE_CONTENT, null),
  /** Tham số hệ thống không tồn tại (UC54). */
  SYSTEM_PARAMETER_NOT_FOUND(HttpStatus.NOT_FOUND, null),
  /** Giá trị tham số hệ thống không hợp lệ hoặc vi phạm ràng buộc miền giá trị / thứ tự (BR-38). */
  SYSTEM_PARAMETER_INVALID_VALUE(HttpStatus.UNPROCESSABLE_CONTENT, null),
  /** Xung đột phiên bản cập nhật tham số hệ thống do có người khác cập nhật trước (Optimistic Locking). */
  SYSTEM_PARAMETER_CONFLICT(HttpStatus.CONFLICT, null);

  private final HttpStatus httpStatus;
  private final String messageCode;

  ErrorCode(HttpStatus httpStatus, String messageCode) {
    this.httpStatus = httpStatus;
    this.messageCode = messageCode;
  }
}
