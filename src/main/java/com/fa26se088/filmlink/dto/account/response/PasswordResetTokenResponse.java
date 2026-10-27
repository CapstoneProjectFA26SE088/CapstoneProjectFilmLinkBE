package com.fa26se088.filmlink.dto.account.response;

/**
 * SCR-03: token đặt lại mật khẩu tạm thời trả về sau khi người dùng xác thực OTP thành công.
 *
 * <p>Token này là một JWT ký HS256 có thời hạn ngắn (15 phút), dùng một lần duy nhất để gửi lên
 * endpoint đặt mật khẩu mới ở bước 3.
 *
 * @param resetToken chuỗi token xác thực đặt lại mật khẩu
 */
public record PasswordResetTokenResponse(String resetToken) {}
