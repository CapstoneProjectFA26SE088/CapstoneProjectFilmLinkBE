package com.fa26se088.filmlink.utils;

import java.util.Locale;

/** Hàm tiện ích cho địa chỉ email. */
public final class EmailUtils {

  private EmailUtils() {}

  /**
   * Chuẩn hoá email trước khi lưu và tra cứu: bỏ khoảng trắng hai đầu, chuyển chữ thường. Bảng
   * {@code user_accounts} có CHECK {@code email = lower(email)}.
   */
  public static String normalize(String email) {
    return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
  }
}
