package com.fa26se088.filmlink.service.account.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.dto.account.request.RegisterRequest;
import com.fa26se088.filmlink.dto.account.request.VerifyEmailRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.dto.account.response.UserAccountResponse;
import com.fa26se088.filmlink.entity.account.OtpCode;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.mapper.account.UserAccountMapper;
import com.fa26se088.filmlink.repository.account.OtpCodeRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.service.account.AuthenticationService;
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
            new UserAccountMapper());
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
}
