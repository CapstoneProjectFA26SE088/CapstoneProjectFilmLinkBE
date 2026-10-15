package com.fa26se088.filmlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Tắt {@code UserDetailsServiceAutoConfiguration}: đăng nhập do {@code AuthenticationService} tự
 * xử lý bằng JWT, không cần tài khoản in-memory "user" với mật khẩu sinh ngẫu nhiên của Spring Boot.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class FilmLinkApplication {

  public static void main(String[] args) {
    SpringApplication.run(FilmLinkApplication.class, args);
  }
}
