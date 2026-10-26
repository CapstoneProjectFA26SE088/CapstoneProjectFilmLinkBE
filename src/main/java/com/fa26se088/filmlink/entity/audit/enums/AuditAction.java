package com.fa26se088.filmlink.entity.audit.enums;

/**
 * Thao tác nhạy cảm được ghi audit log (BR-32), lưu ở cột {@code audit_logs.action}
 * (varchar(100), không có CHECK).
 *
 * <p>Đặt tên theo mẫu {@code <DOMAIN>_<ĐỘNG_TỪ_QUÁ_KHỨ>}, chia nhóm theo domain như {@code
 * ErrorCode}. Chỉ thêm giá trị khi có code thật ghi thao tác đó; domain nào cần thì tự thêm vào
 * nhóm của mình.
 */
public enum AuditAction {
  // --- account ---
  ACCOUNT_PASSWORD_CHANGED,

  // --- masterdata (MD-02, UC54) ---
  MASTERDATA_CREATED,
  MASTERDATA_UPDATED,
  MASTERDATA_STATUS_CHANGED,
  MASTERDATA_DELETE_REJECTED,
  SYSTEM_PARAMETER_UPDATED
}
