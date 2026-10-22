package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * SCR-03 bước 3: đặt mật khẩu mới với token xác thực đã được cấp từ bước 2.
 *
 * <p>Mật khẩu tối đa 72 ký tự vì BCrypt chỉ xử lý 72 byte đầu và từ chối chuỗi dài hơn.
 *
 * @param resetToken token xác thực nhận được từ bước 2
 * @param newPassword mật khẩu mới (tối thiểu 8 ký tự, tối đa 72 ký tự, gồm cả chữ và số)
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token đặt lại mật khẩu không được để trống")
    String resetToken,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu mới phải có từ 8 đến 72 ký tự")
    @Pattern(
        regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
        message = "Mật khẩu mới phải có cả chữ và số")
    String newPassword) {}
