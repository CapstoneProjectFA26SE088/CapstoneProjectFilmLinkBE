package com.fa26se088.filmlink.service.account;

import com.fa26se088.filmlink.dto.account.request.ChangePasswordRequest;
import com.fa26se088.filmlink.dto.account.request.RegisterRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyEmailRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;

/** Vòng đời tài khoản: đăng ký Photographer, xác thực email, hồ sơ của chính mình (UC06), đổi mật khẩu (UC02). */
public interface UserAccountService {

  /**
   * UC06 bước 1–2: tạo tài khoản PENDING_VERIFICATION và gửi OTP email (MSG05).
   *
   * <p>BR-01: role luôn là PHOTOGRAPHER. Email đã tồn tại trả lỗi chung chung {@code
   * ACCOUNT_REGISTRATION_REJECTED}.
   */
  UserAccountResponse register(RegisterRequest request);

  /**
   * UC06 bước 3: OTP đúng thì kích hoạt tài khoản và tự đăng nhập. OTP sai, hết hạn hoặc đã sai 5
   * lần trả {@code ACCOUNT_OTP_INVALID} (MSG11).
   */
  TokenResponse verifyEmail(VerifyEmailRequest request);

  /** Thông tin tài khoản đang đăng nhập. */
  UserAccountResponse getCurrentAccount(Long accountId);

  /**
   * UC02: đổi mật khẩu tài khoản.
   *
   * <p>Kiểm tra mật khẩu cũ, mật khẩu mới khác cũ và tuân thủ quy tắc. Khi đổi thành công, thu
   * hồi toàn bộ refresh token của tài khoản, gửi email cảnh báo bảo mật và ghi audit log.
   *
   * @param accountId ID tài khoản đang đăng nhập
   * @param request thông tin mật khẩu cũ và mật khẩu mới
   */
  void changePassword(Long accountId, ChangePasswordRequest request);
}
