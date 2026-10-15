package com.fa26se088.filmlink.dto.account.response;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import java.time.Instant;

/** Thông tin tài khoản trả cho client. Không chứa mật khẩu hay token (NFR-PRI-04). */
public record UserAccountResponse(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    UserAccountRole role,
    UserAccountStatus status,
    Instant emailVerifiedAt,
    Instant createdAt) {}
