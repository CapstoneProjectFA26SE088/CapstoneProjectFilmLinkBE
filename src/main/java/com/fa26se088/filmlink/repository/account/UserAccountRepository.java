package com.fa26se088.filmlink.repository.account;

import com.fa26se088.filmlink.entity.account.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  /** Email phải được chuẩn hoá (trim + chữ thường) trước khi gọi. */
  Optional<UserAccount> findByEmail(String email);

  /** Email phải được chuẩn hoá (trim + chữ thường) trước khi gọi. */
  boolean existsByEmail(String email);
}
