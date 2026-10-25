package com.fa26se088.filmlink.entity.audit.enums;

/**
 * Loại bản ghi bị tác động, lưu ở cột {@code audit_logs.target_type} (varchar(50), không có CHECK).
 * Cùng với {@code target_id} tạo thành khoá tra lịch sử một bản ghi ({@code ix_audit_logs_target}).
 *
 * <p>Tên là entity theo glossary, viết UPPER_SNAKE_CASE. Chia nhóm theo domain như {@code
 * ErrorCode}; domain nào cần thì tự thêm vào nhóm của mình.
 */
public enum AuditTargetType {
  // --- account ---
  USER_ACCOUNT,

  // --- lab ---
  FILM_LAB
}
