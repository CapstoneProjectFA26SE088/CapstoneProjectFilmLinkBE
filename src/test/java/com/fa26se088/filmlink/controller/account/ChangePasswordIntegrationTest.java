package com.fa26se088.filmlink.controller.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm thử tích hợp UC02 Đổi mật khẩu tài khoản:
 *
 * <ul>
 *   <li>Kiểm tra mật khẩu cũ sai trả 400 và ghi audit failure.
 *   <li>Kiểm tra mật khẩu mới trùng mật khẩu cũ trả 400.
 *   <li>Kiểm tra quy tắc mật khẩu (Bean validation) trả 400 VALIDATION_ERROR.
 *   <li>Đổi mật khẩu thành công: thu hồi mọi refresh token (đăng xuất mọi phiên khác), gửi email cảnh
 *       báo và ghi audit log success.
 *   <li>Đăng nhập lại bằng mật khẩu cũ bị từ chối, mật khẩu mới thành công.
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

  private static final String EMAIL = "changepass-it@example.com";
  private static final String OLD_PASSWORD = "OldPassword123";
  private static final String NEW_PASSWORD = "NewPassword456";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @MockitoBean private EmailClient emailClient;

  private UserAccount account;
  private String accessToken;

  @BeforeEach
  void setUp() {
    account =
        userAccountRepository
            .findByEmail(EMAIL)
            .orElseGet(
                () -> {
                  UserAccount user = new UserAccount();
                  user.setEmail(EMAIL);
                  user.setFullName("Nguyen Van ChangePass");
                  user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
                  user.setRole(UserAccountRole.PHOTOGRAPHER);
                  user.setStatus(UserAccountStatus.ACTIVE);
                  user.setTermsAcceptedAt(Instant.now());
                  user.setEmailVerifiedAt(Instant.now());
                  return userAccountRepository.save(user);
                });

    // Reset về OLD_PASSWORD cho mỗi test
    account.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
    userAccountRepository.save(account);

    // Cấp access token cho request
    accessToken =
        jwtTokenProvider.generateAccessToken(account.getId(), account.getEmail(), account.getRole());
  }

  @Test
  void changePassword_wrongOldPassword_returns400AndRecordsFailureAudit() throws Exception {
    String body =
        """
        {"oldPassword":"IncorrectPassword99","newPassword":"%s"}
        """
            .formatted(NEW_PASSWORD);

    mockMvc
        .perform(
            put("/api/v1/accounts/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_OLD_PASSWORD_INVALID"));

    List<AuditLog> failures =
        auditLogRepository.findAll(
            AuditLogRepository.byTarget(AuditTargetType.USER_ACCOUNT, account.getId()));
    assertThat(failures)
        .anyMatch(
            log ->
                log.getAction() == AuditAction.ACCOUNT_PASSWORD_CHANGED
                    && log.getOutcome() == AuditOutcome.FAILURE
                    && "Mật khẩu cũ không chính xác".equals(log.getReason()));
  }

  @Test
  void changePassword_sameAsOldPassword_returns400() throws Exception {
    String body =
        """
        {"oldPassword":"%s","newPassword":"%s"}
        """
            .formatted(OLD_PASSWORD, OLD_PASSWORD);

    mockMvc
        .perform(
            put("/api/v1/accounts/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NEW_PASSWORD_SAME_AS_OLD"));
  }

  @Test
  void changePassword_invalidNewPasswordFormat_returnsValidationError() throws Exception {
    String body =
        """
        {"oldPassword":"%s","newPassword":"short"}
        """
            .formatted(OLD_PASSWORD);

    mockMvc
        .perform(
            put("/api/v1/accounts/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'newPassword')]").exists());
  }

  @Test
  void changePassword_success_revokesRefreshTokensSendsEmailAndRecordsAudit() throws Exception {
    String rawRefreshToken = "test-raw-refresh-token-" + UUID.randomUUID();

    // 1. Tạo một refresh token đang hoạt động cho phiên đăng nhập này
    RefreshToken activeToken = new RefreshToken();
    activeToken.setUserAccount(account);
    activeToken.setTokenHash(sha256Hex(rawRefreshToken));
    activeToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    refreshTokenRepository.save(activeToken);

    // 2. Thực hiện đổi mật khẩu
    String body =
        """
        {"oldPassword":"%s","newPassword":"%s"}
        """
            .formatted(OLD_PASSWORD, NEW_PASSWORD);

    mockMvc
        .perform(
            put("/api/v1/accounts/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNoContent());

    // 3. Kiểm tra refresh token cũ đã bị thu hồi (revoked_at IS NOT NULL)
    RefreshToken updatedToken = refreshTokenRepository.findById(activeToken.getId()).orElseThrow();
    assertThat(updatedToken.getRevokedAt()).isNotNull();

    // 4. Thử refresh bằng token cũ => 401 ACCOUNT_INVALID_TOKEN (mọi phiên khác bị đăng xuất)
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"refreshToken":"%s"}
                """.formatted(rawRefreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_TOKEN"));

    // 5. Thử đăng nhập bằng mật khẩu cũ => 401 ACCOUNT_INVALID_CREDENTIALS
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s"}
                    """
                        .formatted(EMAIL, OLD_PASSWORD)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_CREDENTIALS"));

    // 6. Đăng nhập bằng mật khẩu mới => 200 OK
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s"}
                    """
                        .formatted(EMAIL, NEW_PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());

    // 7. Kiểm tra email cảnh báo đã gửi
    verify(emailClient)
        .send(
            eq(EMAIL),
            eq("FilmLink - Cảnh báo bảo mật: Mật khẩu đã được thay đổi"),
            anyString());

    // 8. Kiểm tra dòng audit log success (không chứa dữ liệu nhạy cảm)
    List<AuditLog> auditLogs =
        auditLogRepository.findAll(
            AuditLogRepository.byTarget(AuditTargetType.USER_ACCOUNT, account.getId()));
    assertThat(auditLogs)
        .anyMatch(
            log ->
                log.getAction() == AuditAction.ACCOUNT_PASSWORD_CHANGED
                    && log.getOutcome() == AuditOutcome.SUCCESS
                    && "Người dùng tự đổi mật khẩu".equals(log.getReason())
                    && log.getBeforeData() == null
                    && log.getAfterData() == null);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String sha256Hex(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
