package com.fa26se088.filmlink.controller.account;

import com.fa26se088.filmlink.dto.account.request.ChangePasswordRequest;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.account.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản người dùng (UC01, UC02). */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Thông tin tài khoản và đổi mật khẩu (UC02)")
public class UserAccountController {

  private final UserAccountService userAccountService;

  @GetMapping("/me")
  @Operation(summary = "Thông tin tài khoản đang đăng nhập")
  public UserAccountResponse getCurrentAccount(
      @AuthenticationPrincipal AuthenticatedAccount account) {
    return userAccountService.getCurrentAccount(account.id());
  }

  @PutMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Đổi mật khẩu tài khoản (UC02)",
      description =
          "Kiểm tra mật khẩu cũ, mật khẩu mới khác cũ và đủ quy tắc. Thu hồi mọi refresh token đang hoạt động.")
  public void changePassword(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody ChangePasswordRequest request) {
    userAccountService.changePassword(account.id(), request);
  }
}
