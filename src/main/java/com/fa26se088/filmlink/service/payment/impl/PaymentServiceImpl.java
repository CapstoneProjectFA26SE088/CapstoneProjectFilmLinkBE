package com.fa26se088.filmlink.service.payment.impl;

import com.fa26se088.filmlink.client.payment.VnPayClient;
import com.fa26se088.filmlink.dto.payment.request.CreateVnPayPaymentUrlRequest;
import com.fa26se088.filmlink.dto.payment.response.VnPayPaymentUrlResponse;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.service.payment.PaymentService;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SPIKE — chưa nối Service Order, cụm payment chưa thiết kế DB; không dùng cho luồng thật.
 *
 * <p>Không có {@code @Transactional} vì không đụng DB.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private static final String DEFAULT_LOCALE = "vn";
  private static final String TXN_REF_ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final int TXN_REF_LENGTH = 12;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final VnPayClient vnPayClient;

  @Override
  public VnPayPaymentUrlResponse createVnPaySandboxUrl(
      CreateVnPayPaymentUrlRequest request, String clientIp) {
    if (!vnPayClient.isConfigured()) {
      throw new BusinessException(
          ErrorCode.PAYMENT_GATEWAY_NOT_CONFIGURED, "Cổng thanh toán VNPay chưa được cấu hình.");
    }
    String txnRef = generateTxnRef();
    String locale = request.locale() == null ? DEFAULT_LOCALE : request.locale();
    String paymentUrl =
        vnPayClient.buildPaymentUrl(
            txnRef,
            request.amountVnd(),
            "Thanh toan thu nghiem FilmLink " + txnRef,
            request.bankCode(),
            locale,
            clientIp);
    return new VnPayPaymentUrlResponse(paymentUrl);
  }

  /** Mã giao dịch ngẫu nhiên bằng {@link SecureRandom}, 12 ký tự chữ hoa và số. */
  private static String generateTxnRef() {
    StringBuilder builder = new StringBuilder(TXN_REF_LENGTH);
    for (int i = 0; i < TXN_REF_LENGTH; i++) {
      builder.append(TXN_REF_ALPHABET.charAt(SECURE_RANDOM.nextInt(TXN_REF_ALPHABET.length())));
    }
    return builder.toString();
  }
}
