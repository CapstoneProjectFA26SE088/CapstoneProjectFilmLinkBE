package com.fa26se088.filmlink.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  @Test
  void generatedTokenParsesBackToAccountId() {
    JwtTokenProvider provider = provider(randomKey(32), Duration.ofMinutes(15));

    String token = provider.generateAccessToken(42L, "a@example.com", UserAccountRole.ADMIN);

    assertThat(provider.parseAccountId(token)).isEqualTo(42L);
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    String token =
        provider(randomKey(32), Duration.ofMinutes(15))
            .generateAccessToken(42L, "a@example.com", UserAccountRole.ADMIN);

    assertThatThrownBy(() -> provider(randomKey(32), Duration.ofMinutes(15)).parseAccountId(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void expiredTokenIsRejected() {
    JwtTokenProvider provider = provider(randomKey(32), Duration.ofSeconds(-1));
    String token = provider.generateAccessToken(42L, "a@example.com", UserAccountRole.ADMIN);

    assertThatThrownBy(() -> provider.parseAccountId(token)).isInstanceOf(ExpiredJwtException.class);
  }

  /** Khoá dưới 256 bit làm app dừng ngay khi khởi động. */
  @Test
  void weakKeyFailsFast() {
    assertThatThrownBy(() -> provider(randomKey(16), Duration.ofMinutes(15)))
        .isInstanceOf(WeakKeyException.class);
  }

  private static JwtTokenProvider provider(String secret, Duration ttl) {
    return new JwtTokenProvider(new JwtProperties(secret, ttl, Duration.ofDays(7)));
  }

  private static String randomKey(int bytes) {
    byte[] key = new byte[bytes];
    new SecureRandom().nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }
}
