package com.fa26se088.filmlink.entity.account.enums;

/**
 * Trạng thái tài khoản, lưu ở cột {@code user_accounts.status}.
 *
 * <ul>
 *   <li>{@code PENDING_VERIFICATION}: vừa đăng ký, chưa nhập OTP email (UC06).
 *   <li>{@code ACTIVE}: được đăng nhập.
 *   <li>{@code LOCKED}: Admin khoá, kèm {@code lock_reason} (UC11).
 *   <li>{@code DELETED}: đã xoá theo yêu cầu, dữ liệu cá nhân được ẩn danh (NFR-PRI-05).
 * </ul>
 *
 * <p>Khoá tạm do sai mật khẩu nhiều lần không phải trạng thái: dùng cột {@code login_locked_until}.
 */
public enum UserAccountStatus {
  PENDING_VERIFICATION,
  ACTIVE,
  LOCKED,
  DELETED
}
