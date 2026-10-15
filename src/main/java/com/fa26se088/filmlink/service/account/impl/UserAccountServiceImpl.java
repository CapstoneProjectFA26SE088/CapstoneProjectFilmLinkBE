package com.fa26se088.filmlink.service.account.impl;

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
import com.fa26se088.filmlink.service.account.UserAccountService;
import com.fa26se088.filmlink.utils.EmailUtils;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

  /** UC06: OTP hiệu lực 5 phút. */
  static final Duration OTP_TTL = Duration.ofMinutes(5);

  private static final int OTP_BOUND = 1_000_000;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserAccountRepository userAccountRepository;
  private final OtpCodeRepository otpCodeRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailClient emailClient;
  private final AuthenticationService authenticationService;
  private final UserAccountMapper userAccountMapper;

  /**
   * BR-01: tài khoản tự đăng ký luôn có role PHOTOGRAPHER, không nhận role từ request.
   *
   * <p>UC06: email đã tồn tại trả lỗi chung chung để không lộ danh sách người dùng. Hai request
   * đăng ký cùng email chạy song song bị chặn bởi unique {@code uq_user_accounts_email}, cũng trả
   * cùng lỗi đó.
   */
  @Override
  @Transactional
  public UserAccountResponse register(RegisterRequest request) {
    String email = EmailUtils.normalize(request.email());
    String fullName = request.fullName().strip();
    if (fullName.length() < 2) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Họ tên phải có 2–100 ký tự.");
    }
    if (userAccountRepository.existsByEmail(email)) {
      throw registrationRejected();
    }

    UserAccount account = new UserAccount();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(request.password()));
    account.setFullName(fullName);
    account.setPhoneNumber(blankToNull(request.phoneNumber()));
    account.setRole(UserAccountRole.PHOTOGRAPHER);
    account.setStatus(UserAccountStatus.PENDING_VERIFICATION);
    account.setTermsAcceptedAt(Instant.now());
    try {
      userAccountRepository.saveAndFlush(account);
    } catch (DataIntegrityViolationException ex) {
      throw registrationRejected();
    }

    sendEmailVerificationOtp(account);
    return userAccountMapper.toResponse(account);
  }

  /**
   * UC06 bước 3: OTP 6 số, hiệu lực 5 phút, sai tối đa 5 lần. Sai, hết hạn hoặc đã sai đủ 5 lần
   * đều trả MSG11.
   *
   * <p>{@code noRollbackFor}: khi OTP sai, số lần sai phải được lưu dù method ném lỗi; nếu rollback
   * thì người dùng thử được vô hạn lần.
   */
  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public TokenResponse verifyEmail(VerifyEmailRequest request) {
    Instant now = Instant.now();
    UserAccount account =
        userAccountRepository
            .findByEmail(EmailUtils.normalize(request.email()))
            .filter(found -> found.getStatus() == UserAccountStatus.PENDING_VERIFICATION)
            .orElseThrow(UserAccountServiceImpl::otpInvalid);
    OtpCode otpCode =
        otpCodeRepository
            .findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
                account.getId(), OtpPurpose.EMAIL_VERIFICATION)
            .filter(code -> code.isUsableAt(now))
            .orElseThrow(UserAccountServiceImpl::otpInvalid);

    if (!passwordEncoder.matches(request.otp(), otpCode.getCodeHash())) {
      otpCode.setFailedAttempts((short) (otpCode.getFailedAttempts() + 1));
      throw otpInvalid();
    }

    otpCode.setConsumedAt(now);
    account.setStatus(UserAccountStatus.ACTIVE);
    account.setEmailVerifiedAt(now);
    return authenticationService.issueTokens(account.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public UserAccountResponse getCurrentAccount(Long accountId) {
    return userAccountRepository
        .findById(accountId)
        .map(userAccountMapper::toResponse)
        .orElseThrow(
            () ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản."));
  }

  /** UC06 bước 2: sinh OTP bằng {@link SecureRandom}, chỉ lưu BCrypt hash, gửi mã gốc qua email. */
  private void sendEmailVerificationOtp(UserAccount account) {
    String code = String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));
    OtpCode otpCode = new OtpCode();
    otpCode.setUserAccount(account);
    otpCode.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
    otpCode.setCodeHash(passwordEncoder.encode(code));
    otpCode.setExpiresAt(Instant.now().plus(OTP_TTL));
    otpCodeRepository.save(otpCode);

    emailClient.send(
        account.getEmail(),
        "FilmLink - Mã xác thực email",
        "Mã xác thực của bạn là "
            + code
            + ". Mã có hiệu lực trong "
            + OTP_TTL.toMinutes()
            + " phút. Không chia sẻ mã này cho bất kỳ ai.");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static BusinessException registrationRejected() {
    return new BusinessException(
        ErrorCode.ACCOUNT_REGISTRATION_REJECTED, "Không thể đăng ký bằng email này.");
  }

  private static BusinessException otpInvalid() {
    return new BusinessException(
        ErrorCode.ACCOUNT_OTP_INVALID, "Mã xác thực không đúng hoặc đã hết hạn.");
  }
}
