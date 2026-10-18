package com.fa26se088.filmlink.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@link Clock} UTC dùng chung, inject vào chỗ cần thời gian để test thay được (không sleep). */
@Configuration
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
