package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.UserAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}

