package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** UC06 bước 3: nhập mã OTP 6 chữ số gửi qua email. */
public record VerifyEmailRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số") String otp) {}
