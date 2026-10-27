package com.fa26se088.filmlink.service.notification.listener;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import com.fa26se088.filmlink.entity.notification.enums.NotificationType;
import com.fa26se088.filmlink.service.account.event.UserAccountPasswordChangedEvent;
import com.fa26se088.filmlink.service.notification.NotificationService;
import com.fa26se088.filmlink.service.notification.event.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @Mock private NotificationService notificationService;

  private NotificationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificationEventListener(notificationService);
  }

  @Test
  void handleNotificationEvent_delegatesToNotificationService() {
    NotificationEvent event =
        new NotificationEvent(
            42L,
            NotificationType.ORDER_STATUS,
            "Cập nhật đơn hàng",
            "Đơn hàng SO-1234 đã hoàn thành.",
            "https://filmlink.vn/orders/SO-1234");

    listener.handleNotificationEvent(event);

    verify(notificationService)
        .notify(
            42L,
            NotificationType.ORDER_STATUS,
            "Cập nhật đơn hàng",
            "Đơn hàng SO-1234 đã hoàn thành.",
            "https://filmlink.vn/orders/SO-1234");
  }

  @Test
  void handlePasswordChangedEvent_triggersSecurityNotification() {
    UserAccountPasswordChangedEvent event =
        new UserAccountPasswordChangedEvent(15L, "user@example.com", "Le Thi C");

    listener.handlePasswordChangedEvent(event);

    verify(notificationService)
        .notify(
            eq(15L),
            eq(NotificationType.SECURITY),
            eq("FilmLink - Cảnh báo bảo mật: Mật khẩu đã được thay đổi"),
            contains("Mật khẩu tài khoản FilmLink của bạn vừa được thay đổi thành công"),
            isNull());
  }
}
