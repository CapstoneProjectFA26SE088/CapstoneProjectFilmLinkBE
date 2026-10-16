package com.fa26se088.filmlink.security;

import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Lấy tài khoản đang đăng nhập từ {@code SecurityContext}, cho chỗ không phải controller (ví dụ
 * listener, aspect).
 *
 * <p>Controller vẫn dùng {@code @AuthenticationPrincipal AuthenticatedAccount}. Service nghiệp vụ
 * nhận {@code accountId} qua tham số, không gọi class này.
 */
public final class CurrentAccount {

  private CurrentAccount() {}

  /** Tài khoản đang đăng nhập; rỗng khi chưa đăng nhập hoặc là anonymous. */
  public static Optional<AuthenticatedAccount> find() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof AuthenticatedAccount account) {
      return Optional.of(account);
    }
    return Optional.empty();
  }

  /** Như {@link #find()}, nhưng ném {@code AUTHENTICATION_REQUIRED} (401) khi chưa đăng nhập. */
  public static AuthenticatedAccount require() {
    return find()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.AUTHENTICATION_REQUIRED, "Bạn cần đăng nhập để thực hiện thao tác."));
  }
}
