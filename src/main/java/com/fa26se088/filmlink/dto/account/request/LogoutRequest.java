package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC01 bước 3: đăng xuất, thu hồi refresh token của phiên hiện tại. */
public record LogoutRequest(@NotBlank @Size(max = 200) String refreshToken) {}
