package com.fa26se088.filmlink.service.account.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.dto.account.request.LoginRequest;
import com.fa26se088.filmlink.dto.account.request.LogoutRequest;
import com.fa26se088.filmlink.dto.account.request.RefreshTokenRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.entity.account.RefreshToken;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.account.RefreshTokenRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.security.JwtProperties;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

  private static final String EMAIL = "photographer@example.com";
  private static final String PASSWORD = "secret123";
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
  private JwtTokenProvider jwtTokenProvider;
  private AuthenticationServiceImpl service;

  @BeforeEach
  void setUp() {
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    JwtProperties properties =
        new JwtProperties(
            Base64.getEncoder().encodeToString(key), Duration.ofMinutes(15), Duration.ofDays(7));
    jwtTokenProvider = new JwtTokenProvider(properties);
    service =
        new AuthenticationServiceImpl(
            userAccountRepository,
            refreshTokenRepository,
            passwordEncoder,
            jwtTokenProvider,
            properties);
  }

  // --- login (UC01) ---

  /** BR-05 / NFR-SEC-01: token mang id tài khoản, access 15 phút, refresh 7 ngày chỉ lưu hash. */
  @Test
  void login_validCredentials_issuesTokens() {
    UserAccount account = account(UserAccountStatus.ACTIVE);
    when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

    TokenResponse response = service.login(new LoginRequest(" Photographer@Example.com ", PASSWORD));

    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(900);
    assertThat(jwtTokenProvider.parseAccountId(response.accessToken())).isEqualTo(1L);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();
    assertThat(saved.getTokenHash()).hasSize(64).isEqualTo(sha256Hex(response.refreshToken()));
    assertThat(saved.getExpiresAt())
        .isBetween(Instant.now().plus(Duration.ofDays(7)).minusSeconds(5), Instant.now().plus(Duration.ofDays(7)));
    // 32 byte ngẫu nhiên → 43 ký tự Base64URL.
    assertThat(response.refreshToken()).hasSize(43);
  }

  @Test
  void login_wrongPassword_returnsMsg09() {
    when(userAccountRepository.findByEmail(EMAIL))
        .thenReturn(Optional.of(account(UserAccountStatus.ACTIVE)));

    assertError(() -> service.login(new LoginRequest(EMAIL, "wrong-pass1")), ErrorCode.ACCOUNT_INVALID_CREDENTIALS);
    assertThat(ErrorCode.ACCOUNT_INVALID_CREDENTIALS.getMessageCode()).isEqualTo("MSG09");
  }

  @Test
  void login_unknownEmail_returnsSameErrorAsWrongPassword() {
    when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertError(() -> service.login(new LoginRequest(EMAIL, PASSWORD)), ErrorCode.ACCOUNT_INVALID_CREDENTIALS);
  }

  @Test
  void login_deletedAccount_treatedAsInvalidCredentials() {
    when(userAccountRepository.findByEmail(EMAIL))
        .thenReturn(Optional.of(account(UserAccountStatus.DELETED)));

    assertError(() -> service.login(new LoginRequest(EMAIL, PASSWORD)), ErrorCode.ACCOUNT_INVALID_CREDENTIALS);
  }

  @Test
  void login_lockedAccount_returnsLockReasonWithoutTokens() {
    UserAccount account = account(UserAccountStatus.LOCKED);
    account.setLockReason("Vi phạm điều khoản sử dụng");
    when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
              assertThat(ex.getProperties()).containsEntry("lockReason", "Vi phạm điều khoản sử dụng");
            });
    verify(refreshTokenRepository, never()).save(any());
  }

  /** Mật khẩu sai thì không tiết lộ tài khoản đang bị khoá. */
  @Test
  void login_lockedAccountWithWrongPassword_returnsMsg09() {
    when(userAccountRepository.findByEmail(EMAIL))
        .thenReturn(Optional.of(account(UserAccountStatus.LOCKED)));

    assertError(() -> service.login(new LoginRequest(EMAIL, "wrong-pass1")), ErrorCode.ACCOUNT_INVALID_CREDENTIALS);
  }

  @Test
  void login_pendingVerification_returnsNotVerified() {
    when(userAccountRepository.findByEmail(EMAIL))
        .thenReturn(Optional.of(account(UserAccountStatus.PENDING_VERIFICATION)));

    assertError(() -> service.login(new LoginRequest(EMAIL, PASSWORD)), ErrorCode.ACCOUNT_NOT_VERIFIED);
    verify(refreshTokenRepository, never()).save(any());
  }

  // --- refresh (NFR-SEC-01) ---

  @Test
  void refresh_validToken_rotatesAndRevokesOldToken() {
    RefreshToken current = refreshToken(account(UserAccountStatus.ACTIVE), Instant.now().plusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(sha256Hex(RAW_REFRESH_TOKEN)))
        .thenReturn(Optional.of(current));

    TokenResponse response = service.refresh(new RefreshTokenRequest(RAW_REFRESH_TOKEN));

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken next = captor.getValue();
    assertThat(current.getRevokedAt()).isNotNull();
    assertThat(current.getReplacedBy()).isSameAs(next);
    assertThat(next.getTokenHash()).isEqualTo(sha256Hex(response.refreshToken()));
    assertThat(response.refreshToken()).isNotEqualTo(RAW_REFRESH_TOKEN);
  }

  @Test
  void refresh_revokedToken_returns401() {
    RefreshToken current = refreshToken(account(UserAccountStatus.ACTIVE), Instant.now().plusSeconds(60));
    current.setRevokedAt(Instant.now().minusSeconds(1));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));

    assertError(() -> service.refresh(new RefreshTokenRequest(RAW_REFRESH_TOKEN)), ErrorCode.ACCOUNT_INVALID_TOKEN);
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void refresh_expiredToken_returns401() {
    RefreshToken current = refreshToken(account(UserAccountStatus.ACTIVE), Instant.now().minusSeconds(1));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));

    assertError(() -> service.refresh(new RefreshTokenRequest(RAW_REFRESH_TOKEN)), ErrorCode.ACCOUNT_INVALID_TOKEN);
  }

  @Test
  void refresh_unknownToken_returns401() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertError(() -> service.refresh(new RefreshTokenRequest(RAW_REFRESH_TOKEN)), ErrorCode.ACCOUNT_INVALID_TOKEN);
  }

  @Test
  void refresh_lockedAccount_returns401() {
    RefreshToken current = refreshToken(account(UserAccountStatus.LOCKED), Instant.now().plusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));

    assertError(() -> service.refresh(new RefreshTokenRequest(RAW_REFRESH_TOKEN)), ErrorCode.ACCOUNT_INVALID_TOKEN);
  }

  // --- logout (UC01 bước 3) ---

  @Test
  void logout_revokesOwnToken() {
    RefreshToken current = refreshToken(account(UserAccountStatus.ACTIVE), Instant.now().plusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(sha256Hex(RAW_REFRESH_TOKEN)))
        .thenReturn(Optional.of(current));

    service.logout(1L, new LogoutRequest(RAW_REFRESH_TOKEN));

    assertThat(current.getRevokedAt()).isNotNull();
  }

  @Test
  void logout_tokenOfAnotherAccount_isIgnored() {
    RefreshToken current = refreshToken(account(UserAccountStatus.ACTIVE), Instant.now().plusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));

    service.logout(2L, new LogoutRequest(RAW_REFRESH_TOKEN));

    assertThat(current.getRevokedAt()).isNull();
  }

  // --- loadActiveAccount (JwtAuthenticationFilter) ---

  @Test
  void loadActiveAccount_onlyReturnsActiveAccounts() {
    when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account(UserAccountStatus.ACTIVE)));
    assertThat(service.loadActiveAccount(1L))
        .contains(new AuthenticatedAccount(1L, EMAIL, UserAccountRole.PHOTOGRAPHER));

    UserAccount locked = account(UserAccountStatus.LOCKED);
    when(userAccountRepository.findById(1L)).thenReturn(Optional.of(locked));
    assertThat(service.loadActiveAccount(1L)).isEmpty();
  }

  @Test
  void revokeAllTokens_callsRepository() {
    service.revokeAllTokens(1L);
    verify(refreshTokenRepository).revokeAllActiveByAccountId(eq(1L), any(Instant.class));
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(expected);
  }

  private UserAccount account(UserAccountStatus status) {
    UserAccount account = new UserAccount();
    ReflectionTestUtils.setField(account, "id", 1L);
    account.setEmail(EMAIL);
    account.setPasswordHash(passwordEncoder.encode(PASSWORD));
    account.setRole(UserAccountRole.PHOTOGRAPHER);
    account.setStatus(status);
    return account;
  }

  private static RefreshToken refreshToken(UserAccount account, Instant expiresAt) {
    RefreshToken token = new RefreshToken();
    token.setUserAccount(account);
    token.setTokenHash(sha256Hex(RAW_REFRESH_TOKEN));
    token.setExpiresAt(expiresAt);
    return token;
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
