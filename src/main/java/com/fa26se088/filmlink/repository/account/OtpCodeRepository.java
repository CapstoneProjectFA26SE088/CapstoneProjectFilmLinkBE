package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.OtpCode;
import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

  /**
   * Mã mới nhất của một tài khoản theo mục đích (index {@code
   * ix_otp_codes_account_purpose_created}). Khoá dòng để các lần nhập song song không đếm sai
   * {@code failed_attempts}.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OtpCode> findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
      Long userAccountId, OtpPurpose purpose);

  /**
   * UC06 / UH-36: đếm số mã OTP được sinh ra trong cửa sổ thời gian (tối đa 3 lần / 15 phút).
   * Tận dụng index {@code ix_otp_codes_account_purpose_created}.
   */
  long countByUserAccountIdAndPurposeAndCreatedAtAfter(
      Long userAccountId, OtpPurpose purpose, Instant after);

  /**
   * UC06 / UH-36: vô hiệu hoá toàn bộ mã OTP cũ chưa sử dụng của tài khoản theo mục đích
   * (đặt {@code consumed_at = now}) khi yêu cầu gửi lại mã mới.
   */
  @Modifying
  @Query(
      "UPDATE OtpCode o SET o.consumedAt = :now "
          + "WHERE o.userAccount.id = :userAccountId AND o.purpose = :purpose AND o.consumedAt IS NULL")
  int invalidateAllUnconsumed(
      @Param("userAccountId") Long userAccountId,
      @Param("purpose") OtpPurpose purpose,
      @Param("now") Instant now);
}
