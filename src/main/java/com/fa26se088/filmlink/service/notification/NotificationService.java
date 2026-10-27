package com.fa26se088.filmlink.service.notification;

import com.fa26se088.filmlink.entity.notification.enums.NotificationType;

/**
 * Service xử lý thông báo của hệ sinh thái FilmLink (UC04).
 *
 * <p>Căn cứ thiết kế:
 * <ul>
 *   <li><b>UC04 (Receive notifications):</b> Người dùng nhận thông báo hệ thống về đơn hàng, tài khoản,
 *       kiểm duyệt và thanh toán.
 *   <li><b>Giả định G1:</b> Giai đoạn hiện tại hệ thống chỉ gửi thông báo qua email; thông báo in-app
 *       và push notification sẽ được triển khai ở cụm 5.
 *   <li><b>Quy tắc BR / User Preference:</b> Tôn trọng cờ {@code is_email_notification_enabled} của
 *       người dùng. Nếu người dùng tắt nhận thông báo qua email, hệ thống không gửi email thông báo.
 * </ul>
 */
public interface NotificationService {

  /**
   * Gửi thông báo đến tài khoản người dùng qua email, tuân thủ cấu hình {@code
   * is_email_notification_enabled}.
   *
   * @param accountId ID tài khoản người nhận
   * @param type loại thông báo (bảo mật, đơn hàng, thanh toán, kiểm duyệt...)
   * @param title tiêu đề thông báo (dùng làm subject email)
   * @param body nội dung thông báo chính
   * @param link liên kết hành động hoặc chi tiết (nếu có, có thể {@code null})
   */
  void notify(Long accountId, NotificationType type, String title, String body, String link);
}
