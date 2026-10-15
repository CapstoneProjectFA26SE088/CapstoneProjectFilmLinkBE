package com.fa26se088.filmlink.client.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPay, prefix {@code filmlink.vnpay}. {@code tmnCode} và {@code hashSecret} là bí mật,
 * chỉ đọc từ biến môi trường, không có giá trị mặc định.
 *
 * @param tmnCode mã website do VNPay cấp ({@code VNPAY_TMN_CODE})
 * @param hashSecret khoá ký HMAC-SHA512 ({@code VNPAY_HASH_SECRET})
 * @param payUrl URL cổng thanh toán (mặc định sandbox)
 * @param returnUrl URL VNPay chuyển trình duyệt về sau khi thanh toán ({@code VNPAY_RETURN_URL})
 */
@ConfigurationProperties(prefix = "filmlink.vnpay")
public record VnPayProperties(String tmnCode, String hashSecret, String payUrl, String returnUrl) {

  /** Đủ cấu hình để tạo URL thanh toán. */
  public boolean isConfigured() {
    return hasText(tmnCode) && hasText(hashSecret) && hasText(payUrl) && hasText(returnUrl);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
