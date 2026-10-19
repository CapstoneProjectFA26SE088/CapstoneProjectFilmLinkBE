package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  /** Email phải được chuẩn hoá (trim + chữ thường) trước khi gọi. */
  Optional<UserAccount> findByEmail(String email);

  /** Email phải được chuẩn hoá (trim + chữ thường) trước khi gọi. */
  boolean existsByEmail(String email);

  /**
   * UC01 login-lock: nạp tài khoản và khoá dòng ({@code SELECT ... FOR UPDATE}) để hai request
   * đăng nhập cùng email chạy song song không cùng đọc–ghi {@code failed_login_count}, tránh
   * tình huống cả hai đọc count=4 rồi cùng ghi 5 mà không kích hoạt khoá.
   *
   * <p>Email phải được chuẩn hoá (trim + chữ thường) trước khi gọi.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM UserAccount a WHERE a.email = :email")
  Optional<UserAccount> findByEmailForUpdate(@Param("email") String email);

  /**
   * UC06 / AccountPurgeUnverifiedJob: đếm số tài khoản {@link UserAccountStatus#PENDING_VERIFICATION}
   * được tạo trước {@code deadline} — dùng để log số bản ghi trước khi xoá (idempotent nếu chạy
   * lại sau khi xoá đã xong thì trả 0).
   *
   * <p>Tận dụng index {@code ix_user_accounts_unverified_created} (status, created_at).
   */
  @Query(
      "SELECT COUNT(a) FROM UserAccount a"
          + " WHERE a.status = :status AND a.createdAt < :deadline")
  long countByStatusAndCreatedAtBefore(
      @Param("status") UserAccountStatus status, @Param("deadline") Instant deadline);

  /**
   * UC06 / AccountPurgeUnverifiedJob: xoá tài khoản {@link UserAccountStatus#PENDING_VERIFICATION}
   * được tạo trước {@code deadline}. Idempotent: chạy lại khi không còn bản ghi phù hợp thì xoá 0
   * dòng, không lỗi.
   *
   * <p>Tận dụng index {@code ix_user_accounts_unverified_created} (status, created_at).
   *
   * <p>{@code @Modifying(clearAutomatically = true)}: xoá cache first-level sau khi bulk-delete để
   * các query sau trong cùng transaction không đọc thực thể đã bị xoá.
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "DELETE FROM UserAccount a"
          + " WHERE a.status = :status AND a.createdAt < :deadline")
  int deleteByStatusAndCreatedAtBefore(
      @Param("status") UserAccountStatus status, @Param("deadline") Instant deadline);
}
