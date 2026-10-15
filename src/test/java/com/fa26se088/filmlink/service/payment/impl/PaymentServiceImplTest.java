package com.fa26se088.filmlink.service.payment.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.client.payment.VnPayClient;
import com.fa26se088.filmlink.client.payment.VnPayProperties;
import com.fa26se088.filmlink.dto.payment.request.CreateVnPayPaymentUrlRequest;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class PaymentServiceImplTest {

  private static final String PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

  @Test
  void notConfigured_returns503() {
    PaymentServiceImpl service =
        new PaymentServiceImpl(new VnPayClient(new VnPayProperties("", "", PAY_URL, "")));

    assertThatThrownBy(
            () ->
                service.createVnPaySandboxUrl(
                    new CreateVnPayPaymentUrlRequest(100_000L, null, null), "127.0.0.1"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_GATEWAY_NOT_CONFIGURED);
    assertThat(ErrorCode.PAYMENT_GATEWAY_NOT_CONFIGURED.getHttpStatus().value()).isEqualTo(503);
  }

  @Test
  void configured_returnsSandboxUrlWithDefaultLocale() {
    PaymentServiceImpl service =
        new PaymentServiceImpl(
            new VnPayClient(
                new VnPayProperties("TESTCODE", "test-only-secret", PAY_URL, "http://localhost/r")));

    String url =
        service
            .createVnPaySandboxUrl(new CreateVnPayPaymentUrlRequest(100_000L, null, null), "127.0.0.1")
            .paymentUrl();

    assertThat(url).startsWith(PAY_URL + "?").contains("vnp_Locale=vn").contains("vnp_SecureHash=");
  }
}
