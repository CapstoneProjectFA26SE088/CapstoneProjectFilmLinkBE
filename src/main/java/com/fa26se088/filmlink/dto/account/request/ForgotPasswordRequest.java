package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SCR-03: yêu cầu gửi mã OTP đặt lại mật khẩu khi quên mật khẩu.
 *
 * <p>Quy tắc bảo mật: endpoint tiếp nhận request này không bao giờ tiết lộ email có tồn tại trong
 * hệ thống hay không để phòng chống tấn công dò quét tài khoản (enumeration attack).
 *
 * @param email địa chỉ email của tài khoản cần đặt lại mật khẩu
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email) {}
