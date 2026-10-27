package com.fa26se088.filmlink.service.notification.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.dto.account.response.AccountNotificationPreferenceResponse;
import com.fa26se088.filmlink.entity.notification.enums.NotificationType;
import com.fa26se088.filmlink.service.account.UserAccountService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  private static final Long ACCOUNT_ID = 100L;
  private static final String EMAIL = "user@filmlink.test";
  private static final String FULL_NAME = "Nguyen Van B";

  @Mock private UserAccountService userAccountService;
  @Mock private EmailClient emailClient;

  private NotificationServiceImpl notificationService;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationServiceImpl(userAccountService, emailClient);
  }

  @Test
  void notify_whenEmailNotificationEnabled_sendsEmailWithFormattedContent() {
    // Giả lập tài khoản ACTIVE và bật nhận email (is_email_notification_enabled = true)
    when(userAccountService.getNotificationPreference(ACCOUNT_ID))
        .thenReturn(
            Optional.of(
                new AccountNotificationPreferenceResponse(
                    ACCOUNT_ID, EMAIL, FULL_NAME, true, true, true, true)));

    notificationService.notify(
        ACCOUNT_ID,
        NotificationType.ORDER_STATUS,
        "Đơn hàng đã được nhận",
        "Cuộn phim của bạn đã đến Lab và đang chờ xếp lịch tráng.",
        "https://filmlink.vn/orders/SO-1024");

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

    verify(emailClient).send(eq(EMAIL), subjectCaptor.capture(), bodyCaptor.capture());

    assertThat(subjectCaptor.getValue()).isEqualTo("FilmLink - Đơn hàng đã được nhận");
    String emailBody = bodyCaptor.getValue();
    assertThat(emailBody).contains("Xin chào " + FULL_NAME);
    assertThat(emailBody).contains("Cuộn phim của bạn đã đến Lab và đang chờ xếp lịch tráng.");
    assertThat(emailBody).contains("Xem chi tiết tại: https://filmlink.vn/orders/SO-1024");
    assertThat(emailBody).contains("Đội ngũ FilmLink");
  }

  @Test
  void notify_whenEmailNotificationDisabled_doesNotSendEmail() {
    // Giả lập tài khoản tắt nhận thông báo qua email (is_email_notification_enabled = false)
    when(userAccountService.getNotificationPreference(ACCOUNT_ID))
        .thenReturn(
            Optional.of(
                new AccountNotificationPreferenceResponse(
                    ACCOUNT_ID, EMAIL, FULL_NAME, false, true, true, true)));

    notificationService.notify(
        ACCOUNT_ID,
        NotificationType.MARKETPLACE,
        "Có người mua thiết bị ký gửi",
        "Tin đăng Body máy ảnh Leica của bạn đã được thanh toán.",
        null);

    // Tôn trọng is_email_notification_enabled: không gọi EmailClient
    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void notify_whenAccountNotFound_doesNotSendEmail() {
    when(userAccountService.getNotificationPreference(ACCOUNT_ID)).thenReturn(Optional.empty());

    notificationService.notify(
        ACCOUNT_ID,
        NotificationType.SYSTEM,
        "Bảo trì hệ thống",
        "Hệ thống sẽ bảo trì lúc 02:00.",
        null);

    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void notify_whenAccountInactive_doesNotSendEmail() {
    // Tài khoản bị khoá hoặc chưa verify (active = false)
    when(userAccountService.getNotificationPreference(ACCOUNT_ID))
        .thenReturn(
            Optional.of(
                new AccountNotificationPreferenceResponse(
                    ACCOUNT_ID, EMAIL, FULL_NAME, true, true, true, false)));

    notificationService.notify(
        ACCOUNT_ID,
        NotificationType.SYSTEM,
        "Thông báo",
        "Nội dung thông báo",
        null);

    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void notify_whenAccountIdNull_doesNotSendEmail() {
    notificationService.notify(
        null,
        NotificationType.SECURITY,
        "Cảnh báo",
        "Nội dung",
        null);

    verify(userAccountService, never()).getNotificationPreference(null);
    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void notify_whenSubjectAlreadyHasBrand_doesNotDuplicateBrand() {
    when(userAccountService.getNotificationPreference(ACCOUNT_ID))
        .thenReturn(
            Optional.of(
                new AccountNotificationPreferenceResponse(
                    ACCOUNT_ID, EMAIL, FULL_NAME, true, true, true, true)));

    notificationService.notify(
        ACCOUNT_ID,
        NotificationType.SECURITY,
        "FilmLink - Cảnh báo bảo mật",
        "Mật khẩu đã đổi",
        null);

    verify(emailClient).send(eq(EMAIL), eq("FilmLink - Cảnh báo bảo mật"), anyString());
  }
}
