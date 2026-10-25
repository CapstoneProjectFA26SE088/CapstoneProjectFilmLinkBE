package com.fa26se088.filmlink.scheduler;

import com.fa26se088.filmlink.service.account.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FND-03 / UC06: job định kỳ xoá tài khoản {@code PENDING_VERIFICATION} chưa xác thực email sau
 * 24 giờ.
 *
 * <p>Quy ước (xem {@code scheduler/package-info.java}):
 *
 * <ul>
 *   <li>Job mỏng: chỉ gọi {@link UserAccountService#purgeUnverifiedAccounts()}; business rule nằm
 *       trong service.
 *   <li>Cron đọc từ {@code filmlink.jobs.account-purge-unverified.cron} trong {@code
 *       application.yml}.
 *   <li>Zone luôn là {@code Asia/Ho_Chi_Minh} (JVM chạy UTC).
 *   <li>Idempotent: chạy lại không làm sai dữ liệu.
 *   <li>Bật/tắt toàn bộ qua {@code filmlink.scheduling.enabled} ({@code SchedulingConfig}).
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AccountPurgeUnverifiedJob {

  private final UserAccountService userAccountService;

  /**
   * UC06 / FND-03: chạy hàng ngày lúc 2:00 sáng giờ Việt Nam để xoá tài khoản PENDING_VERIFICATION
   * quá 24 giờ chưa xác thực email.
   *
   * <p>Giờ chạy thấp điểm (2:00 AM) tránh tranh chấp tải với người dùng. Cron đọc từ
   * {@code filmlink.jobs.account-purge-unverified.cron}; thay giá trị trong {@code application.yml}
   * để điều chỉnh lịch mà không cần rebuild.
   */
  @Scheduled(
      cron = "${filmlink.jobs.account-purge-unverified.cron}",
      zone = "Asia/Ho_Chi_Minh")
  public void run() {
    userAccountService.purgeUnverifiedAccounts();
  }
}
