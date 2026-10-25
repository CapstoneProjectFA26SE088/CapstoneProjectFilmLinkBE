package com.fa26se088.filmlink.service.lab.impl;

import com.fa26se088.filmlink.service.lab.MachineReadiness;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TẠM THỜI. MCH-01 cung cấp implementation thật trong domain machine (tầng trên được phép phụ thuộc
 * interface tầng dưới), rồi xoá class này.
 *
 * <p>Đếm thẳng trên bảng {@code machines} bằng một câu SQL native, không có entity hay repository
 * của {@code machines} trong domain {@code lab}. Chạy trong transaction của service gọi (JPA và
 * {@link JdbcTemplate} dùng chung connection).
 */
@Component
@RequiredArgsConstructor
public class SqlMachineReadiness implements MachineReadiness {

  private static final String COUNT_ACTIVE_MACHINE_TYPES =
      """
      SELECT count(DISTINCT machine_type)
      FROM machines
      WHERE film_lab_id = ?
        AND status = 'ACTIVE'
        AND machine_type IN ('PROCESSOR', 'SCANNER')
      """;

  private final JdbcTemplate jdbcTemplate;

  /** BR-57: đủ cả hai loại máy PROCESSOR và SCANNER đang ACTIVE. */
  @Override
  public boolean hasActiveProcessorAndScanner(Long filmLabId) {
    Integer types =
        jdbcTemplate.queryForObject(COUNT_ACTIVE_MACHINE_TYPES, Integer.class, filmLabId);
    return types != null && types == 2;
  }
}
