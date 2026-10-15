package com.fa26se088.filmlink.dto.account.response;

/**
 * Cặp token cấp khi đăng nhập, xác thực email hoặc refresh (NFR-SEC-01).
 *
 * @param tokenType luôn là {@code Bearer}
 * @param expiresIn số giây còn hiệu lực của access token
 */
public record TokenResponse(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {}
