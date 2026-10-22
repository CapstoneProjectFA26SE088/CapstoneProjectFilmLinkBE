package com.fa26se088.filmlink.service.notification.listener;

import com.fa26se088.filmlink.entity.notification.enums.NotificationType;
import com.fa26se088.filmlink.service.account.event.UserAccountPasswordChangedEvent;
import com.fa26se088.filmlink.service.notification.NotificationService;
import com.fa26se088.filmlink.service.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener nhận các {@link org.springframework.context.ApplicationEvent} từ các domain khác để
 * gửi thông báo qua {@link NotificationService}.
 *
 * <p>Căn cứ thiết kế:
 * <ul>
 *   <li><b>Mục 5 cấu trúc Backend:</b> Các domain tầng trên hoặc tầng dưới giao tiếp qua
 *       {@code ApplicationEvent}. Tầng nghiệp vụ không gọi trực tiếp dịch vụ gửi thông báo bên ngoài.
 *   <li><b>ERD Conceptual mục 3.20 & 1.5:</b> Sử dụng {@link TransactionalEventListener} với pha
 *       {@link TransactionPhase#AFTER_COMMIT} để đảm bảo thông báo email chỉ được gửi khi
 *       giao dịch cơ sở dữ liệu đã commit thành công. Tránh trường hợp gửi email cảnh báo / xác nhận
 *       nhưng giao dịch sau đó bị rollback.
 *   <li><b>fallbackExecution = true:</b> Đảm bảo listener vẫn được kích hoạt trong trường hợp sự kiện
 *       được phát sinh ngoài phạm vi một Spring transaction đang active.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  /**
   * Xử lý sự kiện thông báo tổng quát {@link NotificationEvent} phát sinh từ bất kỳ domain nào.
   *
   * @param event thông tin sự kiện thông báo
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleNotificationEvent(NotificationEvent event) {
    log.info(
        "[NotificationEventListener] Nhận NotificationEvent: accountId={}, type={}, title='{}'",
        event.accountId(),
        event.type(),
        event.title());

    notificationService.notify(
        event.accountId(),
        event.type(),
        event.title(),
        event.body(),
        event.link());
  }

  /**
   * UC02: Xử lý sự kiện đổi mật khẩu tài khoản {@link UserAccountPasswordChangedEvent} từ domain account.
   *
   * <p>Thay vì domain account gọi trực tiếp {@code EmailClient}, domain account phát sự kiện
   * sau khi cập nhật mật khẩu và thu hồi token thành công. Listener này tiếp nhận và kích hoạt
   * thông báo bảo mật qua {@link NotificationService}.
   *
   * @param event sự kiện đổi mật khẩu thành công
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handlePasswordChangedEvent(UserAccountPasswordChangedEvent event) {
    log.info(
        "[NotificationEventListener] Nhận UserAccountPasswordChangedEvent từ domain account cho accountId={}",
        event.accountId());

    String title = "FilmLink - Cảnh báo bảo mật: Mật khẩu đã được thay đổi";
    String body =
        "Mật khẩu tài khoản FilmLink của bạn vừa được thay đổi thành công. "
            + "Tất cả các phiên đăng nhập khác của bạn đã được đăng xuất để đảm bảo an toàn.\n\n"
            + "Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ ngay với quản trị viên để bảo vệ tài khoản.";

    notificationService.notify(
        event.accountId(),
        NotificationType.SECURITY,
        title,
        body,
        null);
  }
}
