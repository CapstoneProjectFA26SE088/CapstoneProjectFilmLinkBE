package com.fa26se088.filmlink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * NFR-SEC-03: mật khẩu và mã OTP hash bằng BCrypt.
 *
 * <p>Tách khỏi {@link SecurityConfig} để tránh vòng phụ thuộc SecurityConfig → filter → service →
 * PasswordEncoder.
 */
@Configuration
public class PasswordEncoderConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
