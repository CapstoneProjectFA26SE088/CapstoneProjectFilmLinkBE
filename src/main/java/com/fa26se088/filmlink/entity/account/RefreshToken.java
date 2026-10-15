package com.fa26se088.filmlink.entity.account;

import com.fa26se088.filmlink.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Refresh token, bảng {@code refresh_tokens} (NFR-SEC-01: hiệu lực 7 ngày, thu hồi được).
 *
 * <p>Chỉ lưu SHA-256 (hex) của token trong {@link #tokenHash}; chuỗi token gốc chỉ trả cho client
 * một lần. Khi refresh, token cũ bị thu hồi và trỏ sang token mới qua {@link #replacedBy}.
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_account_id", nullable = false)
  private UserAccount userAccount;

  /** Cột {@code char(64)}: cần {@code SqlTypes.CHAR} để {@code ddl-auto: validate} khớp kiểu. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "replaced_by_token_id")
  private RefreshToken replacedBy;

  /** Token còn dùng được: chưa thu hồi và chưa hết hạn tại thời điểm {@code now}. */
  public boolean isUsableAt(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
