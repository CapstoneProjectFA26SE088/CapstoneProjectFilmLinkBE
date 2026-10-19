package com.fa26se088.filmlink.service.account.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.dto.account.request.ChangePasswordRequest;
import com.fa26se088.filmlink.dto.account.request.RegisterRequest;
import com.fa26se088.filmlink.dto.account.request.ResendOtpRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyEmailRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.entity.account.OtpCode;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.mapper.account.UserAccountMapper;
import com.fa26se088.filmlink.repository.account.OtpCodeRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.service.account.AuthenticationService;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
class UserAccountServiceImplTest {

  private static final String EMAIL = "photographer@example.com";
  private static final String OTP = "123456";

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private OtpCodeRepository otpCodeRepository;
  @Mock private EmailClient emailClient;
  @Mock private AuthenticationService authenticationService;
  @Mock private AuditLogService auditLogService;

  /** BCrypt thật (strength thấp cho nhanh) để kiểm hash OTP đúng như khi chạy thật. */
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

  private UserAccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new UserAccountServiceImpl(
            userAccountRepository,
            otpCodeRepository,
            passwordEncoder,
            emailClient,
            authenticationService,
            new UserAccountMapper(),
            auditLogService);
  }

  // --- register (UC06, BR-01) ---

  @Test
  void register_createsPendingPhotographerAndSendsOtp() {
    when(userAccountRepository.existsByEmail(EMAIL)).thenReturn(false);

    UserAccountResponse response =
        service.register(
            new RegisterRequest("  Photographer@Example.COM ", "secret123", " Nguyen An ", "", true));

    ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountRepository).saveAndFlush(accountCaptor.capture());
    UserAccount saved = accountCaptor.getValue();
    assertThat(saved.getEmail()).isEqualTo(EMAIL);
    assertThat(saved.getFullName()).isEqualTo("Nguyen An");
    assertThat(saved.getPhoneNumber()).isNull();
    assertThat(saved.getStatus()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
    assertThat(saved.getTermsAcceptedAt()).isNotNull();
    assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
    assertThat(response.email()).isEqualTo(EMAIL);

    // OTP 6 số gửi qua email; DB chỉ lưu hash, hết hạn sau 5 phút.
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailClient).send(eq(EMAIL), anyString(), bodyCaptor.capture());
    Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(bodyCaptor.getValue());
    assertThat(matcher.find()).isTrue();
    String sentCode = matcher.group(1);

    ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
    verify(otpCodeRepository).save(otpCaptor.capture());
    OtpCode otpCode = otpCaptor.getValue();
    assertThat(otpCode.getPurpose()).isEqualTo(OtpPurpose.EMAIL_VERIFICATION);
    assertThat(otpCode.getCodeHash()).isNotEqualTo(sentCode);
    assertThat(passwordEncoder.matches(sentCode, otpCode.getCodeHash())).isTrue();
    assertThat(otpCode.getExpiresAt())
        .isBetween(Instant.now().plus(Duration.ofMinutes(4)), Instant.now().plus(Duration.ofMinutes(5)));
  }

  /** BR-01: tài khoản tự đăng ký luôn là PHOTOGRAPHER. */
  @Test
  void register_alwaysAssignsPhotographerRole() {
    when(userAccountRepository.existsByEmail(EMAIL)).thenReturn(false);

    UserAccountResponse response =
        service.register(new RegisterRequest(EMAIL, "secret123", "Nguyen An", null, true));

    assertThat(response.role()).isEqualTo(UserAccountRole.PHOTOGRAPHER);
  }

  /** UC06: email trùng trả lỗi chung chung, không tạo tài khoản, không gửi mail. */
  @Test
  void register_duplicateEmail_rejectsWithGenericError() {
    when(userAccountRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(
            () -> service.register(new RegisterRequest(EMAIL, "secret123", "Nguyen An", null, true)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCOUNT_REGISTRATION_REJECTED);
    verify(userAccountRepository, never()).saveAndFlush(any());
    verify(emailClient, never()).send(anyString(), anyString(), anyString());
  }

  // --- verifyEmail (UC06 bước 3) ---

  @Test
  void verifyEmail_correctOtp_activatesAccountAndIssuesTokens() {
    UserAccount account = pendingAccount();
    OtpCode otpCode = otpCode(account, Instant.now().plusSeconds(60), (short) 0);
    stubLookup(account, otpCode);
    TokenResponse tokens = new TokenResponse("access", "refresh", "Bearer", 900);
    when(authenticationService.issueTokens(1L)).thenReturn(tokens);

    TokenResponse result = service.verifyEmail(new VerifyEmailRequest(EMAIL, OTP));

    assertThat(result).isEqualTo(tokens);
    assertThat(account.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    assertThat(account.getEmailVerifiedAt()).isNotNull();
    assertThat(otpCode.getConsumedAt()).isNotNull();
  }

  @Test
  void verifyEmail_wrongOtp_incrementsFailedAttemptsAndReturnsMsg11() {
    UserAccount account = pendingAccount();
    OtpCode otpCode = otpCode(account, Instant.now().plusSeconds(60), (short) 2);
    stubLookup(account, otpCode);

    assertOtpInvalid(() -> service.verifyEmail(new VerifyEmailRequest(EMAIL, "000000")));
    assertThat(otpCode.getFailedAttempts()).isEqualTo((short) 3);
    assertThat(account.getStatus()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
    verify(authenticationService, never()).issueTokens(any());
  }

  @Test
  void verifyEmail_expiredOtp_returnsMsg11() {
    UserAccount account = pendingAccount();
    stubLookup(account, otpCode(account, Instant.now().minusSeconds(1), (short) 0));

    assertOtpInvalid(() -> service.verifyEmail(new VerifyEmailRequest(EMAIL, OTP)));
    assertThat(account.getStatus()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
  }

  /** UC06: đã sai 5 lần thì mã vô hiệu, kể cả khi nhập đúng. */
  @Test
  void verifyEmail_afterFiveFailures_rejectsEvenCorrectOtp() {
    UserAccount account = pendingAccount();
    stubLookup(account, otpCode(account, Instant.now().plusSeconds(60), OtpCode.MAX_FAILED_ATTEMPTS));

    assertOtpInvalid(() -> service.verifyEmail(new VerifyEmailRequest(EMAIL, OTP)));
    assertThat(account.getStatus()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
  }

  @Test
  void verifyEmail_accountAlreadyActive_returnsMsg11() {
    UserAccount account = pendingAccount();
    account.setStatus(UserAccountStatus.ACTIVE);
    when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

    assertOtpInvalid(() -> service.verifyEmail(new VerifyEmailRequest(EMAIL, OTP)));
  }

  // --- resendOtp (UC06, UH-36, MSG12) ---

  /**
   * Yêu cầu gửi lại hợp lệ: đã qua ≥ 60 giây và chưa quá 3 lần / 15 phút.
   * Mã cũ bị vô hiệu hoá và mã mới được gửi qua email.
   */
  @Test
  void resendOtp_validRequest_invalidatesOldOtpsAndSendsNewOne() {
    UserAccount account = pendingAccount();
    when(userAccountRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(account));

    OtpCode previousOtp = otpCode(account, Instant.now().plusSeconds(180), (short) 0);
    ReflectionTestUtils.setField(previousOtp, "createdAt", Instant.now().minusSeconds(70));
    when(otpCodeRepository.findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
            1L, OtpPurpose.EMAIL_VERIFICATION))
        .thenReturn(Optional.of(previousOtp));

    when(otpCodeRepository.countByUserAccountIdAndPurposeAndCreatedAtAfter(
            eq(1L), eq(OtpPurpose.EMAIL_VERIFICATION), any(Instant.class)))
        .thenReturn(1L);

    service.resendOtp(new ResendOtpRequest(EMAIL));

    // Vô hiệu hoá mã cũ
    verify(otpCodeRepository).invalidateAllUnconsumed(eq(1L), eq(OtpPurpose.EMAIL_VERIFICATION), any(Instant.class));
    // Lưu mã mới
    verify(otpCodeRepository).save(any(OtpCode.class));
    // Gửi email mã mới
    verify(emailClient).send(eq(EMAIL), eq("FilmLink - Mã xác thực email"), anyString());
  }

  /**
   * UH-36: gửi lại quá nhanh (< 60 giây) → 429 ACCOUNT_OTP_RESEND_TOO_FAST (MSG12).
   */
  @Test
  void resendOtp_tooFast_throwsAccountOtpResendTooFast() {
    UserAccount account = pendingAccount();
    when(userAccountRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(account));

    OtpCode recentOtp = otpCode(account, Instant.now().plusSeconds(250), (short) 0);
    ReflectionTestUtils.setField(recentOtp, "createdAt", Instant.now().minusSeconds(30)); // 30s trước (< 60s)
    when(otpCodeRepository.findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
            1L, OtpPurpose.EMAIL_VERIFICATION))
        .thenReturn(Optional.of(recentOtp));

    assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest(EMAIL)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OTP_RESEND_TOO_FAST);
              assertThat(ex.getErrorCode().getMessageCode()).isEqualTo("MSG12");
            });

    verify(otpCodeRepository, never()).invalidateAllUnconsumed(any(), any(), any());
    verify(otpCodeRepository, never()).save(any());
    verify(emailClient, never()).send(any(), any(), any());
  }

  /**
   * UH-36: gửi lại quá 3 lần trong 15 phút → 429 ACCOUNT_OTP_RESEND_LIMIT_EXCEEDED (MSG12).
   */
  @Test
  void resendOtp_limitExceeded_throwsAccountOtpResendLimitExceeded() {
    UserAccount account = pendingAccount();
    when(userAccountRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(account));

    OtpCode previousOtp = otpCode(account, Instant.now().plusSeconds(180), (short) 0);
    ReflectionTestUtils.setField(previousOtp, "createdAt", Instant.now().minusSeconds(70));
    when(otpCodeRepository.findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
            1L, OtpPurpose.EMAIL_VERIFICATION))
        .thenReturn(Optional.of(previousOtp));

    when(otpCodeRepository.countByUserAccountIdAndPurposeAndCreatedAtAfter(
            eq(1L), eq(OtpPurpose.EMAIL_VERIFICATION), any(Instant.class)))
        .thenReturn(3L); // Đã có 3 mã trong 15 phút

    assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest(EMAIL)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OTP_RESEND_LIMIT_EXCEEDED);
              assertThat(ex.getErrorCode().getMessageCode()).isEqualTo("MSG12");
            });

    verify(otpCodeRepository, never()).invalidateAllUnconsumed(any(), any(), any());
    verify(otpCodeRepository, never()).save(any());
    verify(emailClient, never()).send(any(), any(), any());
  }

  @Test
  void resendOtp_accountNotFound_throwsResourceNotFound() {
    when(userAccountRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest(EMAIL)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

    verify(otpCodeRepository, never()).save(any());
    verify(emailClient, never()).send(any(), any(), any());
  }

  @Test
  void resendOtp_accountAlreadyActive_throwsBusinessRuleViolation() {
    UserAccount account = pendingAccount();
    account.setStatus(UserAccountStatus.ACTIVE);
    when(userAccountRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest(EMAIL)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));

    verify(otpCodeRepository, never()).save(any());
    verify(emailClient, never()).send(any(), any(), any());
  }

  private void stubLookup(UserAccount account, OtpCode otpCode) {
    when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));
    when(otpCodeRepository.findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
            1L, OtpPurpose.EMAIL_VERIFICATION))
        .thenReturn(Optional.of(otpCode));
  }

  private static void assertOtpInvalid(ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OTP_INVALID);
              assertThat(ex.getErrorCode().getMessageCode()).isEqualTo("MSG11");
            });
  }

  private static UserAccount pendingAccount() {
    UserAccount account = new UserAccount();
    ReflectionTestUtils.setField(account, "id", 1L);
    account.setEmail(EMAIL);
    account.setRole(UserAccountRole.PHOTOGRAPHER);
    account.setStatus(UserAccountStatus.PENDING_VERIFICATION);
    return account;
  }

  private OtpCode otpCode(UserAccount account, Instant expiresAt, short failedAttempts) {
    OtpCode otpCode = new OtpCode();
    otpCode.setUserAccount(account);
    otpCode.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
    otpCode.setCodeHash(passwordEncoder.encode(OTP));
    otpCode.setExpiresAt(expiresAt);
    otpCode.setFailedAttempts(failedAttempts);
    return otpCode;
  }

  // --- changePassword (UC02) ---

  @Test
  void changePassword_success_updatesHashRevokesTokensSendsEmailAndRecordsAudit() {
    UserAccount account = activeAccount("oldSecret123");
    when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    service.changePassword(1L, new ChangePasswordRequest("oldSecret123", "newSecret456"));

    assertThat(passwordEncoder.matches("newSecret456", account.getPasswordHash())).isTrue();
    verify(userAccountRepository).saveAndFlush(account);
    verify(authenticationService).revokeAllTokens(1L);
    verify(emailClient)
        .send(
            eq(EMAIL),
            eq("FilmLink - Cảnh báo bảo mật: Mật khẩu đã được thay đổi"),
            anyString());
    verify(auditLogService)
        .recordSuccess(
            argThat(
                entry ->
                    entry.action() == AuditAction.ACCOUNT_PASSWORD_CHANGED
                        && entry.targetType() == AuditTargetType.USER_ACCOUNT
                        && entry.targetId().equals(1L)
                        && entry.before() == null
                        && entry.after() == null));
  }

  @Test
  void changePassword_wrongOldPassword_recordsFailureAuditAndThrows() {
    UserAccount account = activeAccount("oldSecret123");
    when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    assertThatThrownBy(
            () ->
                service.changePassword(
                    1L, new ChangePasswordRequest("wrongPassword99", "newSecret456")))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OLD_PASSWORD_INVALID));

    verify(auditLogService)
        .recordFailure(
            argThat(
                entry ->
                    entry.action() == AuditAction.ACCOUNT_PASSWORD_CHANGED
                        && entry.targetType() == AuditTargetType.USER_ACCOUNT
                        && entry.targetId().equals(1L)));
    verify(authenticationService, never()).revokeAllTokens(any());
    verify(emailClient, never()).send(any(), any(), any());
    verify(userAccountRepository, never()).save(any());
  }

  @Test
  void changePassword_newPasswordSameAsOld_throwsException() {
    UserAccount account = activeAccount("sameSecret123");
    when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    assertThatThrownBy(
            () ->
                service.changePassword(
                    1L, new ChangePasswordRequest("sameSecret123", "sameSecret123")))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NEW_PASSWORD_SAME_AS_OLD));

    verify(authenticationService, never()).revokeAllTokens(any());
    verify(emailClient, never()).send(any(), any(), any());
    verify(userAccountRepository, never()).save(any());
  }

  @Test
  void changePassword_accountNotFoundOrInactive_throwsNotFound() {
    when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.changePassword(
                    1L, new ChangePasswordRequest("oldSecret123", "newSecret456")))
        .isInstanceOfSatisfying(
            BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

    verify(auditLogService, never()).recordFailure(any());
    verify(auditLogService, never()).recordSuccess(any());
    verify(authenticationService, never()).revokeAllTokens(any());
  }

  private UserAccount activeAccount(String rawPassword) {
    UserAccount account = new UserAccount();
    ReflectionTestUtils.setField(account, "id", 1L);
    account.setEmail(EMAIL);
    account.setFullName("Nguyen Van A");
    account.setPasswordHash(passwordEncoder.encode(rawPassword));
    account.setRole(UserAccountRole.PHOTOGRAPHER);
    account.setStatus(UserAccountStatus.ACTIVE);
    return account;
  }
}
