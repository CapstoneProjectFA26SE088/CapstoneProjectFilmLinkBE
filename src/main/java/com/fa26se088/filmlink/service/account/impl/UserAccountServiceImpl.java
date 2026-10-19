package com.fa26se088.filmlink.service.account.impl;

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
import com.fa26se088.filmlink.service.account.UserAccountService;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import com.fa26se088.filmlink.utils.EmailUtils;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

  /** UC06: OTP hiệu lực 5 phút. */
  static final Duration OTP_TTL = Duration.ofMinutes(5);

  /** UC06 / UH-36: khoảng cách tối thiểu giữa 2 lần gửi OTP liên tiếp (60 giây). */
  static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

  /** UC06 / UH-36: tối đa 3 lần tạo mã OTP trong cửa sổ 15 phút. */
  static final int OTP_MAX_PER_WINDOW = 3;

  /** UC06 / UH-36: độ dài cửa sổ giới hạn gửi lại OTP (15 phút). */
  static final Duration OTP_RESEND_WINDOW = Duration.ofMinutes(15);

  /**
   * UC06 / FND-03: tài khoản PENDING_VERIFICATION chưa xác thực email sau 24 giờ sẽ bị xoá bởi
   * job {@code AccountPurgeUnverifiedJob}.
   */
  static final Duration UNVERIFIED_PURGE_AFTER = Duration.ofHours(24);

  private static final int OTP_BOUND = 1_000_000;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserAccountRepository userAccountRepository;
  private final OtpCodeRepository otpCodeRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailClient emailClient;
  private final AuthenticationService authenticationService;
  private final UserAccountMapper userAccountMapper;
  private final AuditLogService auditLogService;

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

    sendEmailVerificationOtp(account, Instant.now());
    return userAccountMapper.toResponse(account);
  }

  /**
   * UC06 bước 3: OTP 6 số, hiệu lực 5 phút, sai tối đa 5 lần. Sai, hết hạn hoặc đã sai đủ 5 lần
   * đều trả MSG11.
   *
   * <p>{@code noRollbackFor}: khi OTP sai, số lần sai phải được lưu dù method ném lỗi; nếu
   * rollback thì người dùng thử được vô hạn lần.
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

  /**
   * UC06 / UH-36: gửi lại mã OTP qua email.
   *
   * <ul>
   *   <li>Khoá dòng tài khoản ({@code findByEmailForUpdate}) để tuần tự hoá các yêu cầu gửi lại
   *       song song.
   *   <li>Kiểm tra tài khoản tồn tại và đang ở trạng thái {@link
   *       UserAccountStatus#PENDING_VERIFICATION}.
   *   <li>Kiểm tra quy tắc 60 giây (UH-36): khoảng cách với lần tạo mã gần nhất phải ≥ 60 giây.
   *   <li>Kiểm tra quy tắc 15 phút (UH-36): trong 15 phút gần nhất không quá 3 lần tạo mã.
   *   <li>Vô hiệu hoá toàn bộ mã OTP cũ chưa sử dụng (đặt {@code consumed_at = now}).
   *   <li>Sinh mã OTP mới, băm BCrypt, lưu và gửi mã gốc qua email.
   * </ul>
   */
  @Override
  @Transactional
  public void resendOtp(ResendOtpRequest request) {
    Instant now = Instant.now();
    String email = EmailUtils.normalize(request.email());
    OtpPurpose purpose =
        request.purpose() != null ? request.purpose() : OtpPurpose.EMAIL_VERIFICATION;

    UserAccount account =
        userAccountRepository
            .findByEmailForUpdate(email)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản với email này."));

    if (account.getStatus() != UserAccountStatus.PENDING_VERIFICATION) {
      throw new BusinessException(
          ErrorCode.BUSINESS_RULE_VIOLATION, "Tài khoản không ở trạng thái chờ xác thực email.");
    }

    // Quy tắc 60 giây: khoảng cách với lần tạo mã gần nhất phải ≥ 60 giây
    Optional<OtpCode> latestOtp =
        otpCodeRepository.findFirstByUserAccountIdAndPurposeOrderByCreatedAtDesc(
            account.getId(), purpose);
    if (latestOtp.isPresent() && latestOtp.get().getCreatedAt() != null) {
      long secondsSinceLast = Duration.between(latestOtp.get().getCreatedAt(), now).getSeconds();
      if (secondsSinceLast < OTP_RESEND_COOLDOWN_SECONDS) {
        throw new BusinessException(
            ErrorCode.ACCOUNT_OTP_RESEND_TOO_FAST,
            "Vui lòng đợi ít nhất "
                + (OTP_RESEND_COOLDOWN_SECONDS - secondsSinceLast)
                + " giây trước khi yêu cầu gửi lại mã OTP.");
      }
    }

    // Quy tắc tối đa 3 lần / 15 phút
    Instant windowStart = now.minus(OTP_RESEND_WINDOW);
    long countInWindow =
        otpCodeRepository.countByUserAccountIdAndPurposeAndCreatedAtAfter(
            account.getId(), purpose, windowStart);
    if (countInWindow >= OTP_MAX_PER_WINDOW) {
      throw new BusinessException(
          ErrorCode.ACCOUNT_OTP_RESEND_LIMIT_EXCEEDED,
          "Bạn đã vượt quá số lần yêu cầu gửi mã OTP (tối đa "
              + OTP_MAX_PER_WINDOW
              + " lần trong "
              + OTP_RESEND_WINDOW.toMinutes()
              + " phút). Vui lòng thử lại sau.");
    }

    // Vô hiệu hoá mọi mã OTP cũ chưa tiêu thụ
    otpCodeRepository.invalidateAllUnconsumed(account.getId(), purpose, now);

    // Phát hành mã mới và gửi email
    sendEmailVerificationOtp(account, now);
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

  /**
   * UC02: đổi mật khẩu tài khoản.
   *
   * <ul>
   *   <li>Kiểm tra tài khoản tồn tại và đang ACTIVE.
   *   <li>Kiểm tra mật khẩu cũ khớp với hash hiện tại: sai thì ghi audit FAILURE và ném {@code
   *       ACCOUNT_OLD_PASSWORD_INVALID}.
   *   <li>Kiểm tra mật khẩu mới khác mật khẩu cũ: trùng thì ném {@code
   *       ACCOUNT_NEW_PASSWORD_SAME_AS_OLD}.
   *   <li>Lưu mật khẩu mới băm bằng BCrypt.
   *   <li>Thu hồi mọi refresh token qua {@link AuthenticationService#revokeAllTokens(Long)} để
   *       đăng xuất mọi phiên khác (dùng index {@code ix_refresh_tokens_active_by_account}).
   *   <li>Gửi email cảnh báo bảo mật tới địa chỉ của tài khoản.
   *   <li>Ghi audit log SUCCESS (không chứa thông tin mật khẩu).
   * </ul>
   */
  @Override
  @Transactional
  public void changePassword(Long accountId, ChangePasswordRequest request) {
    UserAccount account =
        userAccountRepository
            .findById(accountId)
            .filter(found -> found.getStatus() == UserAccountStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản."));

    if (!passwordEncoder.matches(request.oldPassword(), account.getPasswordHash())) {
      auditLogService.recordFailure(
          new AuditEntry(
              AuditAction.ACCOUNT_PASSWORD_CHANGED,
              AuditTargetType.USER_ACCOUNT,
              accountId,
              "Mật khẩu cũ không chính xác",
              null,
              null));
      throw new BusinessException(
          ErrorCode.ACCOUNT_OLD_PASSWORD_INVALID, "Mật khẩu cũ không chính xác.");
    }

    if (request.oldPassword().equals(request.newPassword())
        || passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
      throw new BusinessException(
          ErrorCode.ACCOUNT_NEW_PASSWORD_SAME_AS_OLD, "Mật khẩu mới phải khác mật khẩu hiện tại.");
    }

    account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.saveAndFlush(account);

    authenticationService.revokeAllTokens(accountId);

    emailClient.send(
        account.getEmail(),
        "FilmLink - Cảnh báo bảo mật: Mật khẩu đã được thay đổi",
        "Xin chào "
            + account.getFullName()
            + ",\n\nMật khẩu tài khoản FilmLink của bạn vừa được thay đổi thành công. "
            + "Tất cả các phiên đăng nhập khác của bạn đã được đăng xuất để đảm bảo an toàn.\n\n"
            + "Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ ngay với quản trị viên để bảo vệ tài khoản.");

    auditLogService.recordSuccess(
        new AuditEntry(
            AuditAction.ACCOUNT_PASSWORD_CHANGED,
            AuditTargetType.USER_ACCOUNT,
            accountId,
            "Người dùng tự đổi mật khẩu",
            null,
            null));
  }

  /**
   * UC06 / FND-03: xoá hàng loạt tài khoản {@code PENDING_VERIFICATION} được tạo từ hơn 24 giờ
   * trước. Idempotent — chạy lại khi không còn bản ghi phù hợp thì xoá 0 dòng, không lỗi.
   *
   * <ul>
   *   <li>Đếm trước để log rõ ràng số bản ghi sẽ bị xoá.
   *   <li>Xoá bằng JPQL bulk-delete (1 câu DELETE duy nhất, tận dụng index {@code
   *       ix_user_accounts_unverified_created}).
   *   <li>Ghi audit SUCCESS — actor là System vì Scheduler không có SecurityContext.
   * </ul>
   */
  @Override
  @Transactional
  public void purgeUnverifiedAccounts() {
    Instant deadline = Instant.now().minus(UNVERIFIED_PURGE_AFTER);

    // Đếm trước để ghi log số bản ghi sẽ bị xoá; idempotent nếu job chạy lại sau khi xoá xong
    long toDelete =
        userAccountRepository.countByStatusAndCreatedAtBefore(
            UserAccountStatus.PENDING_VERIFICATION, deadline);

    log.info(
        "[AccountPurgeUnverifiedJob] Tìm thấy {} tài khoản PENDING_VERIFICATION tạo trước {} cần xoá.",
        toDelete,
        deadline);

    if (toDelete == 0) {
      log.info("[AccountPurgeUnverifiedJob] Không có bản ghi nào cần xoá. Job kết thúc.");
      return;
    }

    int deleted =
        userAccountRepository.deleteByStatusAndCreatedAtBefore(
            UserAccountStatus.PENDING_VERIFICATION, deadline);

    log.info("[AccountPurgeUnverifiedJob] Đã xoá {} tài khoản chưa xác thực.", deleted);

    // Ghi audit actor System (SecurityContext trống trong Scheduler — auditLogService tự ghi System)
    auditLogService.recordSuccess(
        new AuditEntry(
            AuditAction.ACCOUNT_UNVERIFIED_PURGED,
            AuditTargetType.USER_ACCOUNT,
            null,
            "Job xoá " + deleted + " tài khoản PENDING_VERIFICATION tạo trước " + deadline,
            null,
            null));
  }

  /** UC06 bước 2: sinh OTP bằng {@link SecureRandom}, chỉ lưu BCrypt hash, gửi mã gốc qua email. */
  private void sendEmailVerificationOtp(UserAccount account, Instant now) {
    String code = String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));
    OtpCode otpCode = new OtpCode();
    otpCode.setUserAccount(account);
    otpCode.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
    otpCode.setCodeHash(passwordEncoder.encode(code));
    otpCode.setExpiresAt(now.plus(OTP_TTL));
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
