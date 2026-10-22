package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UC02: yêu cầu đổi mật khẩu tài khoản.
 *
 * <p>Mật khẩu tối đa 72 ký tự vì BCrypt chỉ dùng 72 byte đầu và từ chối chuỗi dài hơn.
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu cũ không được để trống")
        @Size(max = 72, message = "Mật khẩu cũ tối đa 72 ký tự")
        String oldPassword,
    @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu mới phải có từ 8 đến 72 ký tự")
        @Pattern(
            regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
            message = "Mật khẩu mới phải có cả chữ và số")
        String newPassword) {}
