package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC01: đăng nhập. Chỉ kiểm định dạng; không kiểm độ mạnh mật khẩu ở đây để không lộ quy tắc
 * khi đăng nhập sai.
 */
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 72) String password) {}
