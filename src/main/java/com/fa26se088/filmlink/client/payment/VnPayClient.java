package com.fa26se088.filmlink.client.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * SPIKE — client VNPay 2.1.0: tạo URL thanh toán và kiểm chữ ký HMAC-SHA512.
 *
 * <p>Chưa nối Service Order, cụm payment chưa thiết kế DB; không dùng cho luồng thật. Chưa có bản
 * mock và chưa xử lý IPN.
 */
@Component
public class VnPayClient {

  private static final String VERSION = "2.1.0";
  private static final String COMMAND_PAY = "pay";
  private static final String CURRENCY_VND = "VND";
  private static final String ORDER_TYPE_OTHER = "other";
  private static final String SECURE_HASH = "vnp_SecureHash";
  private static final String SECURE_HASH_TYPE = "vnp_SecureHashType";
  private static final String HMAC_SHA512 = "HmacSHA512";

  /** VNPay yêu cầu thời gian theo giờ Việt Nam, định dạng {@code yyyyMMddHHmmss}. */
  private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  /** Thời gian URL thanh toán còn hiệu lực. */
  private static final long EXPIRE_MINUTES = 15;

  private final VnPayProperties properties;

  public VnPayClient(VnPayProperties properties) {
    this.properties = properties;
  }

  public boolean isConfigured() {
    return properties.isConfigured();
  }

  /**
   * Tạo URL chuyển người dùng sang cổng VNPay.
   *
   * @param txnRef mã giao dịch phía FilmLink, duy nhất trong ngày
   * @param amountVnd số tiền VND (VNPay nhận đơn vị x100)
   * @param orderInfo mô tả giao dịch, không dấu
   * @param bankCode mã ngân hàng, {@code null} để người dùng tự chọn trên cổng
   * @param locale {@code vn} hoặc {@code en}
   * @param clientIp IP của người thanh toán
   */
  public String buildPaymentUrl(
      String txnRef,
      long amountVnd,
      String orderInfo,
      String bankCode,
      String locale,
      String clientIp) {
    ZonedDateTime now = ZonedDateTime.now(VNPAY_ZONE);
    SortedMap<String, String> params = new TreeMap<>();
    params.put("vnp_Version", VERSION);
    params.put("vnp_Command", COMMAND_PAY);
    params.put("vnp_TmnCode", properties.tmnCode());
    params.put("vnp_Amount", String.valueOf(Math.multiplyExact(amountVnd, 100L)));
    params.put("vnp_CurrCode", CURRENCY_VND);
    if (bankCode != null && !bankCode.isBlank()) {
      params.put("vnp_BankCode", bankCode);
    }
    params.put("vnp_TxnRef", txnRef);
    params.put("vnp_OrderInfo", orderInfo);
    params.put("vnp_OrderType", ORDER_TYPE_OTHER);
    params.put("vnp_Locale", locale);
    params.put("vnp_ReturnUrl", properties.returnUrl());
    params.put("vnp_IpAddr", clientIp);
    params.put("vnp_CreateDate", now.format(VNPAY_TIME));
    params.put("vnp_ExpireDate", now.plusMinutes(EXPIRE_MINUTES).format(VNPAY_TIME));

    String query = toQueryString(params);
    return properties.payUrl() + "?" + query + "&" + SECURE_HASH + "=" + sign(query);
  }

  /**
   * Kiểm chữ ký các tham số VNPay gửi về (return URL / IPN). So sánh thời gian hằng để tránh lộ
   * chữ ký qua thời gian phản hồi.
   */
  public boolean verifySignature(Map<String, String> vnpParams) {
    String receivedHash = vnpParams.get(SECURE_HASH);
    if (receivedHash == null) {
      return false;
    }
    SortedMap<String, String> params = new TreeMap<>(vnpParams);
    params.remove(SECURE_HASH);
    params.remove(SECURE_HASH_TYPE);
    String expectedHash = sign(toQueryString(params));
    return MessageDigest.isEqual(
        expectedHash.getBytes(StandardCharsets.US_ASCII),
        receivedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }

  /** Chuỗi ký và query string của VNPay giống nhau: tham số sắp theo tên, giá trị URL-encode. */
  private static String toQueryString(SortedMap<String, String> params) {
    return params.entrySet().stream()
        .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private String sign(String data) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA512);
      mac.init(
          new SecretKeySpec(properties.hashSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
      return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Không ký được dữ liệu VNPay", ex);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.US_ASCII);
  }
}
