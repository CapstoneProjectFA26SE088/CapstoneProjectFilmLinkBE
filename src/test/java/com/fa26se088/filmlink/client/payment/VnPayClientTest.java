package com.fa26se088.filmlink.client.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class VnPayClientTest {

  /** Khoá giả chỉ dùng trong test, không phải bí mật VNPay. */
  private static final VnPayProperties PROPERTIES =
      new VnPayProperties(
          "TESTCODE",
          "test-only-hash-secret",
          "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
          "http://localhost:5173/payment-result");

  private final VnPayClient client = new VnPayClient(PROPERTIES);

  @Test
  void builtUrlCarriesValidSignature() {
    String url = client.buildPaymentUrl("ABC123", 150_000, "Thanh toan thu", null, "vn", "127.0.0.1");

    Map<String, String> params = queryParams(url);
    assertThat(params).containsEntry("vnp_Amount", "15000000").containsEntry("vnp_TxnRef", "ABC123");
    assertThat(client.verifySignature(params)).isTrue();
  }

  @Test
  void tamperedParameterFailsSignature() {
    Map<String, String> params =
        queryParams(client.buildPaymentUrl("ABC123", 150_000, "Thanh toan thu", null, "vn", "127.0.0.1"));
    params.put("vnp_Amount", "100");

    assertThat(client.verifySignature(params)).isFalse();
  }

  @Test
  void notConfiguredWhenSecretMissing() {
    VnPayClient unconfigured =
        new VnPayClient(new VnPayProperties("", "", PROPERTIES.payUrl(), ""));

    assertThat(unconfigured.isConfigured()).isFalse();
    assertThat(client.isConfigured()).isTrue();
  }

  private static Map<String, String> queryParams(String url) {
    return Arrays.stream(URI.create(url).getRawQuery().split("&"))
        .map(pair -> pair.split("=", 2))
        .collect(
            Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8),
                (a, b) -> b,
                HashMap::new));
  }
}
