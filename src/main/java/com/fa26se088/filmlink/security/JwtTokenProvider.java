package com.fa26se088.filmlink.security;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Tạo và kiểm access token JWT ký HS256 (jjwt 0.13).
 *
 * <p>Payload: {@code sub} = id tài khoản, claim {@code email} và {@code role} (BR-05: token mang
 * role của tài khoản). Claim {@code role} chỉ để FE hiển thị; khi xác thực request, quyền luôn lấy
 * lại từ DB (xem {@link JwtAuthenticationFilter}).
 */
@Component
public class JwtTokenProvider {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_ROLE = "role";

  private final SecretKey signingKey;
  private final Duration accessTokenTtl;

  /**
   * Khoá ngắn hơn 256 bit làm {@code Keys.hmacShaKeyFor} ném {@code WeakKeyException}: app dừng
   * ngay khi khởi động thay vì chạy với khoá yếu.
   */
  public JwtTokenProvider(JwtProperties properties) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    this.accessTokenTtl = properties.accessTokenTtl();
  }

  /** NFR-SEC-01: access token hết hạn sau {@code filmlink.jwt.access-token-ttl} (15 phút). */
  public String generateAccessToken(Long accountId, String email, UserAccountRole role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(accountId))
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_ROLE, role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTokenTtl)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Kiểm chữ ký, hạn dùng và trả id tài khoản trong {@code sub}.
   *
   * @throws JwtException token sai chữ ký, hết hạn hoặc sai định dạng
   * @throws IllegalArgumentException token rỗng hoặc {@code sub} không phải số
   */
  public Long parseAccountId(String token) {
    Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    return Long.valueOf(claims.getSubject());
  }

  public Duration getAccessTokenTtl() {
    return accessTokenTtl;
  }

  private static final String CLAIM_PURPOSE = "purpose";
  private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
  private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(15);

  /**
   * SCR-03: sinh token đặt lại mật khẩu ngắn hạn (15 phút) sau khi xác thực OTP thành công.
   * Token chứa claim {@code purpose = PASSWORD_RESET}.
   *
   * @param accountId ID tài khoản
   * @param email email của tài khoản
   * @return chuỗi token JWT ký HS256
   */
  public String generatePasswordResetToken(Long accountId, String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(accountId))
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_RESET)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(PASSWORD_RESET_TOKEN_TTL)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * SCR-03: xác thực chữ ký, thời hạn và mục đích của password reset token.
   *
   * @param token chuỗi token đặt lại mật khẩu
   * @return accountId của tài khoản
   * @throws JwtException nếu token sai chữ ký, hết hạn hoặc sai mục đích
   */
  public Long parsePasswordResetToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    String purpose = claims.get(CLAIM_PURPOSE, String.class);
    if (!PURPOSE_PASSWORD_RESET.equals(purpose)) {
      throw new JwtException("Token không đúng mục đích đặt lại mật khẩu");
    }
    return Long.valueOf(claims.getSubject());
  }
}
