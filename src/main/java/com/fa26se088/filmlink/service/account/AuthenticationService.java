package com.fa26se088.filmlink.service.account;

import com.fa26se088.filmlink.dto.account.request.LoginRequest;
import com.fa26se088.filmlink.dto.account.request.LogoutRequest;
import com.fa26se088.filmlink.dto.account.request.RefreshTokenRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import java.util.Optional;

/** Phiên đăng nhập: cấp, xoay vòng, thu hồi token (UC01, NFR-SEC-01). */
public interface AuthenticationService {

  /** UC01: đăng nhập bằng email và mật khẩu. */
  TokenResponse login(LoginRequest request);

  /** NFR-SEC-01: đổi refresh token còn hiệu lực lấy cặp token mới; token cũ bị thu hồi. */
  TokenResponse refresh(RefreshTokenRequest request);

  /** UC01 bước 3: thu hồi refresh token của tài khoản đang đăng nhập. */
  void logout(Long accountId, LogoutRequest request);

  /**
   * Cấp cặp token cho một tài khoản ACTIVE. Dùng sau khi đã xác thực xong bằng cách khác (ví dụ
   * OTP ở UC06 bước 3); phải gọi trong transaction đang mở.
   */
  TokenResponse issueTokens(Long accountId);

  /**
   * Nạp principal cho {@code JwtAuthenticationFilter}. Trả rỗng nếu tài khoản không tồn tại hoặc
   * không ACTIVE.
   */
  Optional<AuthenticatedAccount> loadActiveAccount(Long accountId);
}
