package com.fa26se088.filmlink.service.account.impl;

import com.fa26se088.filmlink.dto.account.request.LoginRequest;
import com.fa26se088.filmlink.dto.account.request.LogoutRequest;
import com.fa26se088.filmlink.dto.account.request.RefreshTokenRequest;
import com.fa26se088.filmlink.dto.account.response.TokenResponse;
import com.fa26se088.filmlink.entity.account.RefreshToken;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.account.RefreshTokenRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.security.JwtProperties;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import com.fa26se088.filmlink.service.account.AuthenticationService;
import com.fa26se088.filmlink.utils.EmailUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  static final String TOKEN_TYPE = "Bearer";

  /** 32 byte = 256 bit ngẫu nhiên cho refresh token. */
  private static final int REFRESH_TOKEN_BYTES = 32;

  /**
   * NFR-SEC-07: 5 lần sai liên tiếp trong cửa sổ 15 phút → khoá 15 phút.
   *
   * <p>Window và lock-duration giống nhau (15 phút) đơn giản hoá logic: mỗi lần thất bại đều
   * gia hạn {@code loginLockedUntil} sang {@code NOW + 15 phút}; khi hết thời gian khoá, bộ
   * đếm được reset trong lần đăng nhập thành công tiếp theo.
   */
  static final int LOGIN_MAX_FAILURES = 5;

  static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);

  /**
   * Hash giả BCrypt dùng để so khớp timing-safe khi email không tồn tại (NFR-SEC-07).
   *
   * <p>Khi email không tồn tại trong DB, ta vẫn gọi {@code passwordEncoder.matches()} với hash
   * giả này thay vì bỏ qua. Điều đó khiến thời gian phản hồi xấp xỉ nhau giữa hai trường hợp
   * (email tồn tại vs. không tồn tại), ngăn kẻ tấn công đo thời gian để suy ra email nào có
   * trong hệ thống.
   */
  static final String DUMMY_HASH =
      "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserAccountRepository userAccountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;

  /**
   * UC01: đăng nhập bằng email và mật khẩu với bảo vệ khoá tạm thời (NFR-SEC-07).
   *
   * <p><b>Thứ tự kiểm tra (tuân thủ timing-safe):</b>
   *
   * <ol>
   *   <li>Nạp tài khoản bằng {@code SELECT FOR UPDATE} để ngăn race condition đếm thất bại.
   *   <li>So khớp mật khẩu trên tài khoản thật, hoặc so khớp {@link #DUMMY_HASH} khi email không
   *       tồn tại để thời gian phản hồi không tiết lộ sự tồn tại của email.
   *   <li>Nếu mật khẩu sai: tăng {@code failedLoginCount}; khi đủ {@value #LOGIN_MAX_FAILURES}
   *       lần, đặt {@code loginLockedUntil = NOW + 15 phút}; ném MSG09 (luôn trả cùng mã lỗi).
   *   <li>Khi mật khẩu đúng: kiểm tra khoá tạm thời, rồi kiểm tra trạng thái tài khoản.
   *   <li>Đăng nhập thành công: reset {@code failedLoginCount = 0} và {@code loginLockedUntil = null}.
   * </ol>
   *
   * <p><b>Lưu ý:</b> DELETED luôn trả MSG09. LOCKED trả 403 kèm lockReason chỉ khi mật khẩu đúng.
   */
  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public TokenResponse login(LoginRequest request) {
    Instant now = Instant.now();
    String email = EmailUtils.normalize(request.email());

    // SELECT FOR UPDATE: ngăn hai request cùng email đọc–ghi failedLoginCount song song.
    Optional<UserAccount> found =
        userAccountRepository
            .findByEmailForUpdate(email)
            .filter(a -> a.getStatus() != UserAccountStatus.DELETED);

    // Timing-safe: luôn gọi passwordEncoder.matches dù email không tồn tại,
    // để thời gian phản hồi không cho biết email có tồn tại hay không.
    String hashToCheck = found.map(UserAccount::getPasswordHash).orElse(DUMMY_HASH);
    boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

    if (found.isEmpty() || !passwordMatches) {
      // Chỉ tăng counter khi tài khoản tồn tại thật sự.
      found.ifPresent(account -> recordFailedLogin(account, now));
      throw invalidCredentials();
    }

    UserAccount account = found.get();

    // Kiểm tra khoá tạm thời sau khi xác nhận mật khẩu đúng:
    // Nếu kiểm tra trước khi verify mật khẩu thì kẻ tấn công biết tài khoản đang bị khoá
    // mà không cần biết mật khẩu (phân biệt qua thời gian phản hồi rất ngắn).
    if (account.getLoginLockedUntil() != null && now.isBefore(account.getLoginLockedUntil())) {
      throw new BusinessException(
          ErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
          "Tài khoản bị khoá đăng nhập tạm thời do nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau.");
    }

    // Mật khẩu đúng và không trong thời gian khoá → kiểm tra trạng thái do Admin quản lý.
    switch (account.getStatus()) {
      case LOCKED ->
          throw new BusinessException(
              ErrorCode.ACCOUNT_LOCKED,
              "Tài khoản đã bị khoá.",
              Map.of("lockReason", account.getLockReason()));
      case PENDING_VERIFICATION ->
          throw new BusinessException(
              ErrorCode.ACCOUNT_NOT_VERIFIED, "Tài khoản chưa xác thực email.");
      default -> {
        // ACTIVE: tiếp tục cấp token.
      }
    }

    // Đăng nhập thành công: reset bộ đếm thất bại.
    resetFailedLogin(account);
    return createTokens(account, now).response();
  }

  /**
   * NFR-SEC-01: xoay vòng refresh token. Token cũ bị thu hồi và trỏ sang token mới ({@code
   * replaced_by_token_id}). Token không tồn tại, đã thu hồi, hết hạn hoặc thuộc tài khoản không
   * ACTIVE đều trả 401.
   */
  @Override
  @Transactional
  public TokenResponse refresh(RefreshTokenRequest request) {
    Instant now = Instant.now();
    RefreshToken current =
        refreshTokenRepository
            .findByTokenHash(sha256Hex(request.refreshToken()))
            .filter(token -> token.isUsableAt(now))
            .filter(token -> token.getUserAccount().getStatus() == UserAccountStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.ACCOUNT_INVALID_TOKEN, "Refresh token không hợp lệ."));

    IssuedTokens issued = createTokens(current.getUserAccount(), now);
    current.setRevokedAt(now);
    current.setReplacedBy(issued.refreshToken());
    return issued.response();
  }

  /**
   * UC01 bước 3: thu hồi refresh token gửi lên. Idempotent: token không tồn tại, đã thu hồi hoặc
   * thuộc tài khoản khác thì bỏ qua, vẫn trả thành công.
   */
  @Override
  @Transactional
  public void logout(Long accountId, LogoutRequest request) {
    refreshTokenRepository
        .findByTokenHash(sha256Hex(request.refreshToken()))
        .filter(token -> token.getUserAccount().getId().equals(accountId))
        .filter(token -> token.getRevokedAt() == null)
        .ifPresent(token -> token.setRevokedAt(Instant.now()));
  }

  @Override
  @Transactional
  public TokenResponse issueTokens(Long accountId) {
    UserAccount account =
        userAccountRepository
            .findById(accountId)
            .filter(found -> found.getStatus() == UserAccountStatus.ACTIVE)
            .orElseThrow(
                () -> new IllegalStateException("Chỉ cấp token cho tài khoản ACTIVE: " + accountId));
    return createTokens(account, Instant.now()).response();
  }

  /**
   * UC02, UC11: thu hồi mọi refresh token đang còn hiệu lực của tài khoản (dùng index
   * {@code ix_refresh_tokens_active_by_account}).
   */
  @Override
  @Transactional
  public void revokeAllTokens(Long accountId) {
    refreshTokenRepository.revokeAllActiveByAccountId(accountId, Instant.now());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AuthenticatedAccount> loadActiveAccount(Long accountId) {
    return userAccountRepository
        .findById(accountId)
        .filter(account -> account.getStatus() == UserAccountStatus.ACTIVE)
        .map(account -> new AuthenticatedAccount(account.getId(), account.getEmail(), account.getRole()));
  }

  // ─── Helpers: login-lock (NFR-SEC-07) ──────────────────────────────────────

  /**
   * Ghi nhận một lần đăng nhập thất bại (NFR-SEC-07).
   *
   * <ul>
   *   <li>Nếu khoá tạm thời trước đó đã hết hạn hoặc lần sai gần nhất đã quá 15 phút: reset đếm về 1.
   *   <li>Ngược lại: tăng {@code failedLoginCount} lên 1.
   *   <li>Khi đủ {@value #LOGIN_MAX_FAILURES} lần: đặt {@code loginLockedUntil = now + 15 phút}.
   * </ul>
   *
   * <p>Gọi trong transaction đang mở; Hibernate flush tự động khi commit.
   */
  private void recordFailedLogin(UserAccount account, Instant now) {
    boolean lockExpired =
        account.getLoginLockedUntil() != null && now.isAfter(account.getLoginLockedUntil());
    boolean windowExpired =
        account.getUpdatedAt() != null
            && Duration.between(account.getUpdatedAt(), now).compareTo(LOGIN_LOCK_DURATION) > 0;

    short newCount;
    if (lockExpired || windowExpired) {
      newCount = 1;
      account.setLoginLockedUntil(null);
    } else {
      newCount = (short) (account.getFailedLoginCount() + 1);
    }

    account.setFailedLoginCount(newCount);
    if (newCount >= LOGIN_MAX_FAILURES) {
      account.setLoginLockedUntil(now.plus(LOGIN_LOCK_DURATION));
    }
  }

  /**
   * Reset bộ đếm thất bại sau khi đăng nhập thành công.
   *
   * <p>Dọn cả {@code loginLockedUntil} để khoá không còn hiệu lực kể từ lần này.
   */
  private static void resetFailedLogin(UserAccount account) {
    account.setFailedLoginCount((short) 0);
    account.setLoginLockedUntil(null);
  }

  private static BusinessException invalidCredentials() {
    return new BusinessException(
        ErrorCode.ACCOUNT_INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng.");
  }

  // ─── Helpers: token issuance ────────────────────────────────────────────────

  /**
   * Tạo access token và refresh token mới. Refresh token là 256 bit ngẫu nhiên (Base64URL); DB chỉ
   * lưu SHA-256 hex, chuỗi gốc chỉ trả về client.
   */
  private IssuedTokens createTokens(UserAccount account, Instant now) {
    byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUserAccount(account);
    refreshToken.setTokenHash(sha256Hex(rawRefreshToken));
    refreshToken.setExpiresAt(now.plus(jwtProperties.refreshTokenTtl()));
    refreshTokenRepository.save(refreshToken);

    String accessToken =
        jwtTokenProvider.generateAccessToken(account.getId(), account.getEmail(), account.getRole());
    TokenResponse response =
        new TokenResponse(
            accessToken,
            rawRefreshToken,
            TOKEN_TYPE,
            jwtTokenProvider.getAccessTokenTtl().toSeconds());
    return new IssuedTokens(refreshToken, response);
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("JVM thiếu SHA-256", ex);
    }
  }

  /** Kết quả cấp token: entity refresh token mới (để nối {@code replacedBy}) và response. */
  private record IssuedTokens(RefreshToken refreshToken, TokenResponse response) {}
}
