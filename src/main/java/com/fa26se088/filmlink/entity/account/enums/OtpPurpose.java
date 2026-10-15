package com.fa26se088.filmlink.entity.account.enums;

/** Mục đích của mã OTP, lưu ở cột {@code otp_codes.purpose}. */
public enum OtpPurpose {
  /** UC06: xác thực email khi đăng ký. */
  EMAIL_VERIFICATION,
  /** SCR-03: quên mật khẩu. */
  PASSWORD_RESET
}
