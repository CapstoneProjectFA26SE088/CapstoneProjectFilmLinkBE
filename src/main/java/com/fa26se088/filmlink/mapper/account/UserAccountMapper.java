package com.fa26se088.filmlink.mapper.account;

import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.entity.account.UserAccount;
import org.springframework.stereotype.Component;

/** Chuyển {@link UserAccount} sang DTO. Viết tay, không dùng MapStruct. */
@Component
public class UserAccountMapper {

  public UserAccountResponse toResponse(UserAccount account) {
    return new UserAccountResponse(
        account.getId(),
        account.getEmail(),
        account.getFullName(),
        account.getPhoneNumber(),
        account.getRole(),
        account.getStatus(),
        account.getEmailVerifiedAt(),
        account.getCreatedAt());
  }
}
