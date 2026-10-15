package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Tra token theo SHA-256 hash (unique {@code uq_refresh_tokens_hash}). Khoá dòng ({@code SELECT
   * ... FOR UPDATE}) để hai request refresh cùng một token chạy song song không cùng thành công.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
