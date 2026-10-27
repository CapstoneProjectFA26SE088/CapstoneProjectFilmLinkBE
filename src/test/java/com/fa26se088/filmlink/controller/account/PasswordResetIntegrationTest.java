package com.fa26se088.filmlink.controller.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.entity.account.RefreshToken;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.entity.audit.AuditLog;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditOutcome;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.repository.account.OtpCodeRepository;
import com.fa26se088.filmlink.repository.account.RefreshTokenRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.repository.audit.AuditLogRepository;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Kiểm thử tích hợp luồng Quên mật khẩu 3 bước (SCR-03, UC02):
 *
 * <ol>
 *   <li>POST /api/v1/auth/forgot-password → 204, gửi OTP qua email.
 *   <li>POST /api/v1/auth/verify-password-reset-otp → 200, trả resetToken 15 phút.
 *   <li>POST /api/v1/auth/reset-password → 204, thu hồi refresh token cũ, ghi audit log.
 * </ol>
 *
 * <p>Bảo vệ chống enumeration (anti-enumeration): email không tồn tại vẫn trả 204 và <em>không</em>
 * gửi email.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

  /** Email tài khoản test; được tạo một lần duy nhất trong {@link #setUp()}. */
  private static final String EMAIL = "forgot-pass-it@example.com";

  /** Mật khẩu ban đầu của tài khoản test. */
  private static final String OLD_PASSWORD = "OldPass123";

  /** Mật khẩu mới sẽ đặt trong bước 3 của luồng. */
  private static final String NEW_PASSWORD = "NewPass456";

  /** Regex 6 chữ số để extract OTP từ email body (bám sát pattern AuthFlowIntegrationTest). */
  private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private OtpCodeRepository otpCodeRepository;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  /**
   * Mock EmailClient để capture OTP gửi đi mà không cần SMTP thật. Bám sát cách dùng của
   * ChangePasswordIntegrationTest.
   */
  @MockitoBean private EmailClient emailClient;

  /** Tài khoản test được tái sử dụng xuyên suốt các @Test. */
  private UserAccount account;

  /**
   * Chuẩn bị tài khoản ACTIVE với mật khẩu {@link #OLD_PASSWORD} trước mỗi test. Tài khoản đã xác
   * thực email để có thể đăng nhập bình thường.
   */
  @BeforeEach
  void setUp() {
    // Lấy hoặc tạo mới tài khoản test để tránh duplicate email
    account =
        userAccountRepository
            .findByEmail(EMAIL)
            .orElseGet(
                () -> {
                  UserAccount user = new UserAccount();
                  user.setEmail(EMAIL);
                  user.setFullName("Nguyen Van ForgotPass");
                  user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
                  user.setRole(UserAccountRole.PHOTOGRAPHER);
                  user.setStatus(UserAccountStatus.ACTIVE);
                  user.setTermsAcceptedAt(Instant.now());
                  user.setEmailVerifiedAt(Instant.now());
                  return userAccountRepository.save(user);
                });

    // Reset mật khẩu và trạng thái cho mỗi test (tránh ô nhiễm dữ liệu giữa các test)
    account.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
    account.setFailedLoginCount((short) 0); // UserAccount.failedLoginCount là Short
    account.setLoginLockedUntil(null);
    userAccountRepository.save(account);

    // Xoá OTP cũ để bước forgot-password không bị cooldown
    otpCodeRepository.deleteAll(
        otpCodeRepository.findAll().stream()
            .filter(o -> o.getUserAccount().getId().equals(account.getId()))
            .toList());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Happy path: luồng 3 bước hoàn chỉnh
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Kiểm tra toàn bộ luồng quên mật khẩu thành công:
   *
   * <ol>
   *   <li>Bước 1: gửi OTP → 204, emailClient được gọi đúng một lần.
   *   <li>Bước 2: xác thực OTP → 200 với resetToken hợp lệ (không null, không rỗng).
   *   <li>Bước 3: đặt mật khẩu mới → 204, refresh token cũ bị thu hồi, audit log ghi nhận.
   *   <li>Sau bước 3: đăng nhập mật khẩu cũ → 401; mật khẩu mới → 200 OK.
   * </ol>
   */
  @Test
  void fullPasswordResetFlow_success() throws Exception {
    // ── Seed một refresh token cũ để kiểm tra thu hồi ────────────────────────
    RefreshToken oldToken = buildRefreshToken(account, "some-old-raw-token");
    refreshTokenRepository.save(oldToken);
    long tokenCountBefore = refreshTokenRepository.findAll().stream()
        .filter(t -> t.getUserAccount().getId().equals(account.getId()))
        .count();
    assertThat(tokenCountBefore).isPositive();

    // ── Bước 1: forgot-password ───────────────────────────────────────────────
    postJson("/api/v1/auth/forgot-password", forgotBody(EMAIL))
        .andExpect(status().isNoContent()); // 204 dù email tồn tại hay không

    // Capture OTP từ email gửi đi
    String otp = captureOtp(EMAIL);
    assertThat(otp).matches("\\d{6}");

    // ── Bước 2: verify-password-reset-otp ────────────────────────────────────
    String responseJson =
        postJson("/api/v1/auth/verify-password-reset-otp", verifyOtpBody(EMAIL, otp))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resetToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract resetToken từ JSON response
    String resetToken =
        com.jayway.jsonpath.JsonPath.read(responseJson, "$.resetToken");
    assertThat(resetToken).isNotBlank();

    // ── Bước 3: reset-password ────────────────────────────────────────────────
    postJson("/api/v1/auth/reset-password", resetBody(resetToken, NEW_PASSWORD))
        .andExpect(status().isNoContent()); // 204

    // Kiểm tra mật khẩu đã được hash mới trong DB
    UserAccount updated = userAccountRepository.findByEmail(EMAIL).orElseThrow();
    assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches(OLD_PASSWORD, updated.getPasswordHash())).isFalse();

    // Kiểm tra tất cả refresh token cũ bị thu hồi (revokedAt != null)
    List<RefreshToken> tokens = refreshTokenRepository.findAll().stream()
        .filter(t -> t.getUserAccount().getId().equals(account.getId()))
        .toList();
    assertThat(tokens).allMatch(t -> t.getRevokedAt() != null);

    // Kiểm tra audit log ACCOUNT_PASSWORD_RESET được ghi
    List<AuditLog> logs = auditLogRepository.findAll(
        AuditLogRepository.byTarget(AuditTargetType.USER_ACCOUNT, account.getId()));
    assertThat(logs)
        .anyMatch(
            log ->
                log.getAction() == AuditAction.ACCOUNT_PASSWORD_RESET
                    && log.getOutcome() == AuditOutcome.SUCCESS);

    // ── Sau reset: đăng nhập mật khẩu cũ → 401 ──────────────────────────────
    postJson("/api/v1/auth/login", loginBody(OLD_PASSWORD))
        .andExpect(status().isUnauthorized());

    // Đăng nhập mật khẩu mới → thành công
    postJson("/api/v1/auth/login", loginBody(NEW_PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Anti-enumeration: email không tồn tại phải trả 204 và không gửi email
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Yêu cầu OTP bằng email không tồn tại → phải trả 204 giống hệt email tồn tại (chống dò tìm
   * email trong hệ thống). EmailClient không được gọi.
   */
  @Test
  void forgotPassword_nonExistentEmail_returns204AndSendsNoEmail() throws Exception {
    postJson("/api/v1/auth/forgot-password", forgotBody("ghost-nonexistent@example.com"))
        .andExpect(status().isNoContent()); // 204 – phản hồi giống hệt email tồn tại

    // Đảm bảo không có email thật nào được gửi đi
    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Lỗi bước 2: OTP sai → 422
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Nhập OTP sai → 422 ACCOUNT_OTP_INVALID. Số lần sai phải được lưu vào DB để phòng brute force
   * ngay cả khi transaction lỗi (noRollbackFor = BusinessException.class).
   */
  @Test
  void verifyPasswordResetOtp_wrongOtp_returns422() throws Exception {
    // Bước 1 trước: tạo OTP hợp lệ trong DB
    postJson("/api/v1/auth/forgot-password", forgotBody(EMAIL))
        .andExpect(status().isNoContent());

    // Bước 2 với OTP sai
    postJson("/api/v1/auth/verify-password-reset-otp", verifyOtpBody(EMAIL, "000000"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_OTP_INVALID"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Lỗi bước 3: reset token không hợp lệ → 401
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Gửi reset token giả mạo (không hợp lệ) → 401 ACCOUNT_INVALID_TOKEN. Đảm bảo không thể đặt
   * lại mật khẩu khi không có token hợp lệ.
   */
  @Test
  void resetPassword_invalidToken_returns401() throws Exception {
    postJson("/api/v1/auth/reset-password", resetBody("this-is-a-fake-token", NEW_PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_TOKEN"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Validation: mật khẩu mới không đạt quy tắc → 400
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Mật khẩu mới không đủ mạnh (ít hơn 8 ký tự) → 400 VALIDATION_ERROR. Kiểm tra Bean Validation
   * ở tầng controller.
   */
  @Test
  void resetPassword_weakNewPassword_returns400ValidationError() throws Exception {
    // Lấy resetToken hợp lệ qua bước 1–2
    postJson("/api/v1/auth/forgot-password", forgotBody(EMAIL))
        .andExpect(status().isNoContent());
    String otp = captureOtp(EMAIL);

    String responseJson =
        postJson("/api/v1/auth/verify-password-reset-otp", verifyOtpBody(EMAIL, otp))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String resetToken = com.jayway.jsonpath.JsonPath.read(responseJson, "$.resetToken");

    // Thử đặt mật khẩu "short" (thiếu chữ hoa, thiếu số, < 8 ký tự)
    postJson("/api/v1/auth/reset-password", resetBody(resetToken, "short"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ─────────────────────────────────────────────────────────────────────────────

  /** Gửi POST JSON đến path và trả về ResultActions để chain assertion. */
  private ResultActions postJson(String path, String body) throws Exception {
    return mockMvc.perform(
        post(path).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  /**
   * Capture OTP 6 chữ số từ email body được gửi đến {@code recipientEmail}. Bám sát cách dùng
   * ArgumentCaptor trong AuthFlowIntegrationTest.
   */
  private String captureOtp(String recipientEmail) {
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    // Verify emailClient.send(to, subject, body) được gọi ít nhất 1 lần
    verify(emailClient, org.mockito.Mockito.atLeastOnce())
        .send(eq(recipientEmail), anyString(), bodyCaptor.capture());
    List<String> capturedBodies = bodyCaptor.getAllValues();
    // Lấy email body mới nhất (trường hợp gửi nhiều lần)
    String lastBody = capturedBodies.get(capturedBodies.size() - 1);
    Matcher matcher = OTP_PATTERN.matcher(lastBody);
    assertThat(matcher.find()).as("OTP 6 chữ số phải có trong email body").isTrue();
    return matcher.group(1);
  }

  /**
   * Tạo một RefreshToken hợp lệ gắn với tài khoản, dùng để kiểm tra thu hồi sau reset password.
   * Hash theo SHA-256 như AuthenticationServiceImpl thực tế làm.
   */
  private RefreshToken buildRefreshToken(UserAccount owner, String rawToken) {
    RefreshToken token = new RefreshToken();
    token.setUserAccount(owner);
    token.setTokenHash(sha256Hex(rawToken));
    token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    return token;
  }

  /** SHA-256 hex của rawValue, bám sát cách AuthenticationServiceImpl lưu token. */
  private static String sha256Hex(String rawValue) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(rawValue.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 không khả dụng", ex);
    }
  }

  /** Request body cho POST /forgot-password. */
  private static String forgotBody(String email) {
    return """
        {"email":"%s"}
        """.formatted(email);
  }

  /** Request body cho POST /verify-password-reset-otp. */
  private static String verifyOtpBody(String email, String otp) {
    return """
        {"email":"%s","otp":"%s"}
        """.formatted(email, otp);
  }

  /** Request body cho POST /reset-password. */
  private static String resetBody(String resetToken, String newPassword) {
    return """
        {"resetToken":"%s","newPassword":"%s"}
        """.formatted(resetToken, newPassword);
  }

  /** Request body cho POST /login. */
  private String loginBody(String password) {
    return """
        {"email":"%s","password":"%s"}
        """.formatted(EMAIL, password);
  }
}
