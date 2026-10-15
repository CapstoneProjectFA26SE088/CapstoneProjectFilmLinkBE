package com.fa26se088.filmlink.dto.payment.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * SPIKE — tham số tạo URL thanh toán VNPay sandbox. Chưa gắn Service Order.
 *
 * @param amountVnd số tiền VND, 10.000–50.000.000
 * @param bankCode mã ngân hàng VNPay (tuỳ chọn), ví dụ {@code NCB}
 * @param locale {@code vn} hoặc {@code en}; bỏ trống thì dùng {@code vn}
 */
public record CreateVnPayPaymentUrlRequest(
    @NotNull @Min(10_000) @Max(50_000_000) Long amountVnd,
    @Pattern(regexp = "^[A-Za-z0-9]{2,20}$") String bankCode,
    @Pattern(regexp = "^(vn|en)$") String locale) {}
