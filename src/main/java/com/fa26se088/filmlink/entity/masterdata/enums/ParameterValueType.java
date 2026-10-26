package com.fa26se088.filmlink.entity.masterdata.enums;

/**
 * Kiểu dữ liệu của tham số hệ thống trong bảng {@code system_parameters.value_type}.
 * Ràng buộc bởi CHECK constraint: INTEGER, PERCENT, DECIMAL, STRING.
 */
public enum ParameterValueType {
  INTEGER,
  PERCENT,
  DECIMAL,
  STRING
}
