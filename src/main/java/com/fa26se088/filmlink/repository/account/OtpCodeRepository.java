package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.OtpCode;
import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

  /**
   * Mã mới nhất của một tài khoản theo mục đích (index {@code
   * ix_otp_codes_account_purpose_created}). Khoá dòng để các lần nhập song song không đếm sai
   * {@code failed_attempts}.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OtpCode> findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
      Long userAccountId, OtpPurpose purpose);
}
