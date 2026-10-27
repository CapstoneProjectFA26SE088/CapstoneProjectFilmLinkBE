package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * SCR-03 bước 2: xác thực mã OTP 6 số để đặt lại mật khẩu.
 *
 * @param email địa chỉ email của tài khoản
 * @param otp mã OTP 6 số nhận được qua email
 */
public record VerifyPasswordResetOtpRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email,

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
    String otp) {}
