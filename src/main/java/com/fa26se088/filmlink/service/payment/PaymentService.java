package com.fa26se088.filmlink.service.payment;

import com.fa26se088.filmlink.dto.payment.request.CreateVnPayPaymentUrlRequest;
import com.fa26se088.filmlink.dto.payment.response.VnPayPaymentUrlResponse;

/**
 * Thanh toán.
 *
 * <p>Hiện chỉ có SPIKE VNPay sandbox: chưa nối Service Order, cụm payment chưa thiết kế DB; không
 * dùng cho luồng thật.
 */
public interface PaymentService {

  /**
   * SPIKE — tạo URL thanh toán VNPay sandbox để thử tích hợp. Không lưu giao dịch.
   *
   * @param clientIp IP của người thanh toán, VNPay yêu cầu
   */
  VnPayPaymentUrlResponse createVnPaySandboxUrl(
      CreateVnPayPaymentUrlRequest request, String clientIp);
}
