package com.fa26se088.filmlink.client.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bản mock của {@link EmailClient}: ghi email (kể cả mã OTP) ra log thay vì gửi thật.
 *
 * <p>Chỉ được tạo khi {@code filmlink.mail.provider=log}. Không bao giờ bật trên môi trường thật
 * (NFR-PRI-04: không log OTP).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "filmlink.mail.provider", havingValue = "log")
public class LoggingEmailClient implements EmailClient {

  @Override
  public void send(String recipient, String subject, String body) {
    log.info("[MAIL provider=log] To: {} | Subject: {}\n{}", recipient, subject, body);
  }
}
