package com.fa26se088.filmlink.service.account;

import com.fa26se088.filmlink.dto.account.request.ChangePasswordRequest;
import com.fa26se088.filmlink.dto.account.request.ForgotPasswordRequest;
import com.fa26se088.filmlink.dto.account.request.RegisterRequest;
import com.fa26se088.filmlink.dto.account.request.ResendOtpRequest;
import com.fa26se088.filmlink.dto.account.request.ResetPasswordRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyEmailRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyPasswordResetOtpRequest;
import com.fa26se088.filmlink.dto.account.response.AccountNotificationPreferenceResponse;
import com.fa26se088.filmlink.dto.account.response.PasswordResetTokenResponse;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import java.util.Optional;

/** Vòng đời tài khoản: đăng ký Photographer, xác thực email, hồ sơ của chính mình (UC06), đổi mật khẩu (UC02), quên mật khẩu (SCR-03). */
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

  /**
   * UC06 / UH-36: gửi lại mã OTP qua email.
   *
   * <ul>
   *   <li>Khoảng cách giữa hai lần gửi liên tiếp phải ≥ 60 giây (nếu không: ném {@code
   *       ACCOUNT_OTP_RESEND_TOO_FAST}, MSG12).
   *   <li>Tối đa 3 lần gửi OTP trong cửa sổ 15 phút (nếu không: ném {@code
   *       ACCOUNT_OTP_RESEND_LIMIT_EXCEEDED}, MSG12).
   *   <li>Vô hiệu hoá toàn bộ mã OTP cũ chưa sử dụng của tài khoản.
   * </ul>
   *
   * @param request thông tin email và mục đích OTP cần gửi lại
   */
  void resendOtp(ResendOtpRequest request);

  /** Thông tin tài khoản đang đăng nhập. */
  UserAccountResponse getCurrentAccount(Long accountId);

  /**
   * Lấy cấu hình nhận thông báo và thông tin người nhận của tài khoản (UC04).
   *
   * <p>Được {@code NotificationService} (domain notification) sử dụng để kiểm tra cờ {@code
   * is_email_notification_enabled} và lấy địa chỉ email gửi thông báo theo quy tắc R4.
   *
   * @param accountId ID tài khoản
   * @return cấu hình thông báo của tài khoản, hoặc {@link Optional#empty()} nếu không tìm thấy
   */
  Optional<AccountNotificationPreferenceResponse> getNotificationPreference(Long accountId);

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

  /**
   * SCR-03 bước 1: yêu cầu gửi mã OTP đặt lại mật khẩu qua email.
   *
   * <p>Quy tắc bảo mật: phương thức không bao giờ tiết lộ email có tồn tại hay không. Dù email
   * có tồn tại hay không trong hệ thống, client đều nhận cùng một phản hồi thành công. Nếu tài
   * khoản tồn tại và đang hoạt động (ACTIVE), hệ thống sinh mã OTP lưu vào bảng {@code otp_codes}
   * với mục đích {@code PASSWORD_RESET} và gửi email cho người dùng.
   *
   * @param request thông tin email cần đặt lại mật khẩu
   */
  void forgotPassword(ForgotPasswordRequest request);

  /**
   * SCR-03 bước 2: xác thực mã OTP đặt lại mật khẩu.
   *
   * <p>Kiểm tra mã OTP 6 số theo mục đích {@code PASSWORD_RESET}. Nếu hợp lệ, đánh dấu mã OTP đã
   * sử dụng (consumed) và cấp token đặt lại mật khẩu (reset token) ngắn hạn (15 phút) cho bước 3.
   *
   * @param request thông tin email và mã OTP
   * @return token đặt lại mật khẩu
   */
  PasswordResetTokenResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request);

  /**
   * SCR-03 bước 3: đặt mật khẩu mới bằng token xác thực nhận được từ bước 2.
   *
   * <p>Xác thực reset token, kiểm tra mật khẩu mới khác mật khẩu cũ, lưu mật khẩu mới đã băm
   * BCrypt, thu hồi toàn bộ refresh token cũ của tài khoản, phát sự kiện gửi email cảnh báo bảo
   * mật và ghi audit log.
   *
   * @param request thông tin token xác thực và mật khẩu mới
   */
  void resetPassword(ResetPasswordRequest request);

  /**
   * UC06 / FND-03: xoá hàng loạt tài khoản {@code PENDING_VERIFICATION} được tạo trước {@code
   * deadline} (mặc định = {@code now() − 24h}).
   *
   * <ul>
   *   <li>Idempotent: chạy lại không lỗi.
   *   <li>Log số bản ghi đã xử lý.
   *   <li>Ghi audit actor System ({@code ACCOUNT_UNVERIFIED_PURGED}).
   * </ul>
   *
   * <p>Được gọi từ {@code AccountPurgeUnverifiedJob}; không dùng ở API.
   */
  void purgeUnverifiedAccounts();
}

