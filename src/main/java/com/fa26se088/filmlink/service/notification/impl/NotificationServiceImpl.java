package com.fa26se088.filmlink.service.notification.impl;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.dto.account.response.AccountNotificationPreferenceResponse;
import com.fa26se088.filmlink.entity.notification.enums.NotificationType;
import com.fa26se088.filmlink.service.account.UserAccountService;
import com.fa26se088.filmlink.service.notification.NotificationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Hiện thực {@link NotificationService} cho UC04 theo giả định G1 (chỉ gửi qua EmailClient).
 *
 * <p>Quy tắc và ranh giới kiến trúc:
 * <ul>
 *   <li><b>R4:</b> Không inject trực tiếp {@code UserAccountRepository} của domain account, mà gọi
 *       thông qua {@link UserAccountService#getNotificationPreference(Long)}.
 *   <li><b>R9:</b> Gửi email thông qua abstraction {@link EmailClient} (ở môi trường local dùng
 *       {@code LoggingEmailClient} ghi log, môi trường production dùng SMTP).
 *   <li><b>User Preference:</b> Tôn trọng cờ {@code is_email_notification_enabled}. Nếu người dùng
 *       chủ động tắt nhận email thông báo, hệ thống sẽ bỏ qua việc gửi email.
 *   <li><b>Giả định G1:</b> In-app notification và push notification chưa được lưu trữ DB ở giai
 *       đoạn này mà sẽ được bổ sung ở cụm 5.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final UserAccountService userAccountService;
  private final EmailClient emailClient;

  /**
   * UC04: Gửi thông báo đến người dùng qua email.
   *
   * <ul>
   *   <li>Lấy thông tin tài khoản và cấu hình thông báo từ {@link UserAccountService}.
   *   <li>Nếu tài khoản không tồn tại hoặc không còn hoạt động: ghi log cảnh báo và bỏ qua.
   *   <li>Kiểm tra cờ {@code is_email_notification_enabled}: nếu tắt ({@code false}), ghi log và
   *       không gửi email.
   *   <li>Định dạng nội dung email (tiêu đề, lời chào, nội dung thông báo, liên kết hành động và
   *       chữ ký FilmLink).
   *   <li>Gửi thông qua {@link EmailClient}.
   * </ul>
   */
  @Override
  public void notify(Long accountId, NotificationType type, String title, String body, String link) {
    if (accountId == null) {
      log.warn("[Notification] Không thể gửi thông báo: accountId bị null. Type={}, Title={}", type, title);
      return;
    }

    // 1. Lấy thông tin cấu hình người nhận từ domain account (tuân thủ quy tắc R4)
    Optional<AccountNotificationPreferenceResponse> preferenceOpt =
        userAccountService.getNotificationPreference(accountId);

    if (preferenceOpt.isEmpty()) {
      log.warn(
          "[Notification] Không tìm thấy thông tin tài khoản accountId={} để gửi thông báo. Type={}, Title={}",
          accountId,
          type,
          title);
      return;
    }

    AccountNotificationPreferenceResponse preference = preferenceOpt.get();

    // Kiểm tra trạng thái hoạt động của tài khoản
    if (!preference.active()) {
      log.info(
          "[Notification] Bỏ qua gửi thông báo tới accountId={} do tài khoản không ở trạng thái ACTIVE.",
          accountId);
      return;
    }

    // 2. Tôn trọng cờ is_email_notification_enabled của người dùng
    if (!preference.emailNotificationEnabled()) {
      log.info(
          "[Notification] Bỏ qua gửi email tới accountId={} ({}) do người dùng đã tắt tuỳ chọn nhận thông báo qua email (is_email_notification_enabled = false). Type={}, Title={}",
          accountId,
          preference.email(),
          type,
          title);
      return;
    }

    // 3. Xây dựng nội dung email hoàn chỉnh
    String emailSubject = formatSubject(title);
    String emailBody = formatEmailBody(preference.fullName(), body, link);

    // 4. Gửi qua EmailClient (local = log qua LoggingEmailClient, production = SMTP thật)
    log.info(
        "[Notification] Đang gửi email thông báo [{}] tới accountId={} ({})",
        type,
        accountId,
        preference.email());
    emailClient.send(preference.email(), emailSubject, emailBody);
  }

  /**
   * Định dạng tiêu đề email có nhận diện thương hiệu FilmLink.
   */
  private String formatSubject(String title) {
    if (title == null || title.isBlank()) {
      return "FilmLink - Thông báo mới";
    }
    String stripped = title.strip();
    return stripped.startsWith("FilmLink") ? stripped : "FilmLink - " + stripped;
  }

  /**
   * Định dạng phần thân email chuyên nghiệp gồm lời chào, nội dung, đường dẫn và footer.
   */
  private String formatEmailBody(String recipientName, String body, String link) {
    StringBuilder builder = new StringBuilder();

    if (recipientName != null && !recipientName.isBlank()) {
      builder.append("Xin chào ").append(recipientName.strip()).append(",\n\n");
    } else {
      builder.append("Xin chào bạn,\n\n");
    }

    if (body != null && !body.isBlank()) {
      builder.append(body.strip()).append("\n\n");
    }

    if (link != null && !link.isBlank()) {
      builder.append("Xem chi tiết tại: ").append(link.strip()).append("\n\n");
    }

    builder.append("---\n")
        .append("Trân trọng,\n")
        .append("Đội ngũ FilmLink\n")
        .append("Đây là email tự động từ hệ thống FilmLink, vui lòng không trả lời trực tiếp email này.");

    return builder.toString();
  }
}
