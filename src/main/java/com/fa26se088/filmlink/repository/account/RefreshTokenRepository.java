package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Tra token theo SHA-256 hash (unique {@code uq_refresh_tokens_hash}). Khoá dòng ({@code SELECT
   * ... FOR UPDATE}) để hai request refresh cùng một token chạy song song không cùng thành công.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * UC02 (đổi mật khẩu), UC11 (khoá tài khoản): thu hồi mọi refresh token đang còn hiệu lực của một
   * tài khoản.
   *
   * <p>Truy vấn tận dụng partial index {@code ix_refresh_tokens_active_by_account} (WHERE
   * revoked_at IS NULL).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE RefreshToken r SET r.revokedAt = :revokedAt WHERE r.userAccount.id = :userAccountId AND r.revokedAt IS NULL")
  int revokeAllActiveByAccountId(
      @Param("userAccountId") Long userAccountId, @Param("revokedAt") Instant revokedAt);
}
