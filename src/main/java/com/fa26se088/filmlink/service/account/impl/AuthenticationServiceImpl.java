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

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserAccountRepository userAccountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;

  /**
   * UC01: sai email, sai mật khẩu hoặc tài khoản đã xoá đều trả một lỗi chung MSG09, không cho
   * biết sai ở đâu. Chỉ khi mật khẩu đúng mới báo trạng thái tài khoản:
   *
   * <ul>
   *   <li>LOCKED: 403, kèm {@code lockReason} do Admin nhập, không cấp token.
   *   <li>PENDING_VERIFICATION: 403 {@code ACCOUNT_NOT_VERIFIED}, FE chuyển sang bước nhập OTP.
   * </ul>
   *
   * <p>BR-05: access token mang role của tài khoản.
   */
  @Override
  @Transactional
  public TokenResponse login(LoginRequest request) {
    UserAccount account =
        userAccountRepository
            .findByEmail(EmailUtils.normalize(request.email()))
            .filter(found -> found.getStatus() != UserAccountStatus.DELETED)
            .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.ACCOUNT_INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng."));

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
        // ACTIVE: cấp token bên dưới.
      }
    }
    return createTokens(account, Instant.now()).response();
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

  @Override
  @Transactional(readOnly = true)
  public Optional<AuthenticatedAccount> loadActiveAccount(Long accountId) {
    return userAccountRepository
        .findById(accountId)
        .filter(account -> account.getStatus() == UserAccountStatus.ACTIVE)
        .map(account -> new AuthenticatedAccount(account.getId(), account.getEmail(), account.getRole()));
  }

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
