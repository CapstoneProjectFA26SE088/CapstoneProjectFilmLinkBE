package com.fa26se088.filmlink.dto.account.response;

/**
 * Cấu hình nhận thông báo và thông tin người nhận của tài khoản (UC04).
 *
 * <p>Được {@code NotificationService} sử dụng để lấy địa chỉ email và kiểm tra cờ {@code
 * is_email_notification_enabled} theo quy tắc R4 (không truy cập repository của domain account
 * từ domain notification).
 *
 * @param accountId ID tài khoản
 * @param email địa chỉ email của tài khoản
 * @param fullName họ tên hiển thị của tài khoản
 * @param emailNotificationEnabled cờ cho phép nhận thông báo qua email (cột {@code is_email_notification_enabled})
 * @param inAppNotificationEnabled cờ cho phép nhận thông báo trong ứng dụng (cột {@code is_in_app_notification_enabled})
 * @param pushNotificationEnabled cờ cho phép nhận thông báo đẩy thiết bị (cột {@code is_push_notification_enabled})
 * @param active trạng thái tài khoản có đang hoạt động (ACTIVE) hay không
 */
public record AccountNotificationPreferenceResponse(
    Long accountId,
    String email,
    String fullName,
    boolean emailNotificationEnabled,
    boolean inAppNotificationEnabled,
    boolean pushNotificationEnabled,
    boolean active) {}
