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

  // --- masterdata (MD-02) ---
  MASTERDATA_CREATED,
  MASTERDATA_UPDATED,
  MASTERDATA_STATUS_CHANGED,
  MASTERDATA_DELETE_REJECTED
  /**
   * UC06: job định kỳ xoá hàng loạt tài khoản PENDING_VERIFICATION quá 24 giờ chưa xác thực email.
   * Actor là System (Scheduler). Target type USER_ACCOUNT, targetId = null (batch).
   */
  ACCOUNT_UNVERIFIED_PURGED,

  // --- lab ---
  /** UC12: Film Lab Owner bật/tắt nhận đơn; before/after ghi {@code acceptingOrders}. */
  LAB_ACCEPTING_ORDERS_CHANGED
}
