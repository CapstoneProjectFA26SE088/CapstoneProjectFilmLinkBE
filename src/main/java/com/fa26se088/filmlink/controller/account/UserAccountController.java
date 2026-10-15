package com.fa26se088.filmlink.controller.account;

import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.account.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản người dùng. */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Thông tin tài khoản")
public class UserAccountController {

  private final UserAccountService userAccountService;

  @GetMapping("/me")
  @Operation(summary = "Thông tin tài khoản đang đăng nhập")
  public UserAccountResponse getCurrentAccount(
      @AuthenticationPrincipal AuthenticatedAccount account) {
    return userAccountService.getCurrentAccount(account.id());
  }
}
