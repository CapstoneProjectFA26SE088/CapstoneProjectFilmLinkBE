package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UC06: đăng ký tài khoản Photographer.
 *
 * <p>Mật khẩu tối đa 72 ký tự vì BCrypt chỉ dùng 72 byte đầu và từ chối chuỗi dài hơn.
 */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
            regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
            message = "Mật khẩu phải có cả chữ và số")
        String password,
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @Size(max = 15) String phoneNumber,
    @NotNull @AssertTrue(message = "Phải đồng ý điều khoản sử dụng") Boolean termsAccepted) {}
