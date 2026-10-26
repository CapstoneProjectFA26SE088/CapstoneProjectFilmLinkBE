package com.fa26se088.filmlink.service.masterdata;

import com.fa26se088.filmlink.entity.masterdata.SystemParameter;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import java.math.BigDecimal;

/**
 * Service đọc tham số hệ thống có kiểu dữ liệu an toàn (Type-safe) cho toàn bộ backend (UC54, BR-38).
 * Các domain nghiệp vụ (order, billing, storage, ai...) đọc qua service này bằng {@link SystemParameterKey},
 * không dùng chuỗi String rải rác.
 */
public interface SystemParameterService {

  /** Lấy giá trị tham số dưới dạng số nguyên {@link Integer}. */
  Integer getInteger(SystemParameterKey key);

  /** Lấy giá trị tham số dạng tỉ lệ phần trăm ({@code Double}, ví dụ "10" -> 10.0). */
  Double getPercent(SystemParameterKey key);

  /** Lấy giá trị tham số dạng {@link BigDecimal}. */
  BigDecimal getDecimal(SystemParameterKey key);

  /** Lấy giá trị tham số dạng chuỗi {@link String}. */
  String getString(SystemParameterKey key);

  /** Lấy entity nguyên bản của tham số. */
  SystemParameter getParameter(SystemParameterKey key);
}
