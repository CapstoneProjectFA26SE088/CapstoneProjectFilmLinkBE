package com.fa26se088.filmlink.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình JWT, prefix {@code filmlink.jwt} trong {@code application.yml}.
 *
 * @param secret khoá HS256 dạng Base64, tối thiểu 256 bit. Đọc từ biến môi trường {@code
 *     JWT_SECRET}, không có giá trị mặc định
 * @param accessTokenTtl thời hạn access token (NFR-SEC-01: 15 phút)
 * @param refreshTokenTtl thời hạn refresh token (NFR-SEC-01: 7 ngày)
 */
@Validated
@ConfigurationProperties(prefix = "filmlink.jwt")
public record JwtProperties(
    @NotBlank String secret, @NotNull Duration accessTokenTtl, @NotNull Duration refreshTokenTtl) {}
