package com.fa26se088.filmlink.entity.account;

import com.fa26se088.filmlink.entity.BaseEntity;
import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Mã OTP một lần, bảng {@code otp_codes} (UC06 xác thực email, SCR-03 quên mật khẩu).
 *
 * <p>Chỉ lưu BCrypt hash của mã trong {@link #codeHash}. Mã chỉ còn hiệu lực khi chưa dùng, chưa
 * hết hạn và số lần nhập sai chưa đạt {@link #MAX_FAILED_ATTEMPTS}.
 */
@Getter
@Setter
@Entity
@Table(name = "otp_codes")
public class OtpCode extends BaseEntity {

  /** UC06: tối đa 5 lần nhập sai (CHECK {@code ck_otp_codes_attempts}). */
  public static final short MAX_FAILED_ATTEMPTS = 5;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_account_id", nullable = false)
  private UserAccount userAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 30)
  private OtpPurpose purpose;

  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private Short failedAttempts = 0;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  /** Mã còn được phép nhập thử tại thời điểm {@code now}. */
  public boolean isUsableAt(Instant now) {
    return consumedAt == null && expiresAt.isAfter(now) && failedAttempts < MAX_FAILED_ATTEMPTS;
  }
}
