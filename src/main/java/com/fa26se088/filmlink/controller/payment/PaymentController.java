package com.fa26se088.filmlink.controller.payment;

import com.fa26se088.filmlink.dto.payment.request.CreateVnPayPaymentUrlRequest;
import com.fa26se088.filmlink.dto.payment.response.VnPayPaymentUrlResponse;
import com.fa26se088.filmlink.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thanh toán. Hiện chỉ có SPIKE VNPay sandbox, không dùng cho luồng thật. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "SPIKE VNPay sandbox — chưa nối Service Order")
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * IP lấy từ {@code getRemoteAddr()}: khi chạy sau proxy (Azure), cấu hình {@code
   * server.forward-headers-strategy} thay vì tự đọc header {@code X-Forwarded-For} (giả mạo được).
   */
  @PostMapping("/vnpay/sandbox-url")
  @Operation(
      summary = "SPIKE: tạo URL thanh toán VNPay sandbox",
      description = "Chưa nối Service Order, không lưu giao dịch. 503 nếu chưa cấu hình VNPay.")
  public VnPayPaymentUrlResponse createVnPaySandboxUrl(
      @Valid @RequestBody CreateVnPayPaymentUrlRequest request, HttpServletRequest httpRequest) {
    return paymentService.createVnPaySandboxUrl(request, httpRequest.getRemoteAddr());
  }
}
