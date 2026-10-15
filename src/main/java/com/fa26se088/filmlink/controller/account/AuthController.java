package com.fa26se088.filmlink.controller.account;

import com.fa26se088.filmlink.dto.account.request.LoginRequest;
import com.fa26se088.filmlink.dto.account.request.LogoutRequest;
import com.fa26se088.filmlink.dto.account.request.RefreshTokenRequest;
import com.fa26se088.filmlink.dto.account.request.RegisterRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyEmailRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.account.AuthenticationService;
import com.fa26se088.filmlink.service.account.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Đăng ký, xác thực email, đăng nhập, refresh, đăng xuất (UC01, UC06). */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng ký, đăng nhập và quản lý phiên (UC01, UC06)")
public class AuthController {

  private final UserAccountService userAccountService;
  private final AuthenticationService authenticationService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Đăng ký tài khoản Photographer (UC06)",
      description = "Tạo tài khoản chưa xác thực và gửi OTP qua email (MSG05).")
  public UserAccountResponse register(@Valid @RequestBody RegisterRequest request) {
    return userAccountService.register(request);
  }

  @PostMapping("/verify-email")
  @Operation(
      summary = "Xác thực email bằng OTP (UC06)",
      description = "OTP đúng thì kích hoạt tài khoản và trả token. Sai hoặc hết hạn: MSG11.")
  public TokenResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return userAccountService.verifyEmail(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Đăng nhập (UC01)", description = "Sai email hoặc mật khẩu: MSG09.")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authenticationService.login(request);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Đổi refresh token lấy cặp token mới",
      description = "Refresh token cũ bị thu hồi sau khi dùng (NFR-SEC-01).")
  public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authenticationService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Đăng xuất (UC01)", description = "Thu hồi refresh token gửi lên.")
  public void logout(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody LogoutRequest request) {
    authenticationService.logout(account.id(), request);
  }
}
