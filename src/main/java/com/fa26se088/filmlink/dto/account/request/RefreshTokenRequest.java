package com.fa26se088.filmlink.dto.account.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** NFR-SEC-01: đổi refresh token lấy cặp token mới. */
public record RefreshTokenRequest(@NotBlank @Size(max = 200) String refreshToken) {}
