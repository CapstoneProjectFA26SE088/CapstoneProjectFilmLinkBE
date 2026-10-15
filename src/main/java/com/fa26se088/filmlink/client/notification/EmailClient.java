package com.fa26se088.filmlink.client.notification;

/**
 * Gửi email. Bản dùng được chọn bằng property {@code filmlink.mail.provider}:
 *
 * <ul>
 *   <li>{@code log}: {@link LoggingEmailClient}, chỉ ghi nội dung ra log (máy local).
 *   <li>SMTP thật: chưa có, thêm khi chốt nhà cung cấp.
 * </ul>
 */
public interface EmailClient {

  /**
   * Gửi một email văn bản thuần.
   *
   * @param recipient địa chỉ người nhận
   * @param subject tiêu đề
   * @param body nội dung văn bản
   */
  void send(String recipient, String subject, String body);
}
