package com.fa26se088.filmlink.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho các job trong {@code scheduler/}. Tắt bằng {@code
 * filmlink.scheduling.enabled=false} (test tắt để job không chạy giữa chừng).
 *
 * <p>JVM chạy UTC: mọi {@code @Scheduled(cron = ...)} phải ghi {@code zone = "Asia/Ho_Chi_Minh"}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "filmlink.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
