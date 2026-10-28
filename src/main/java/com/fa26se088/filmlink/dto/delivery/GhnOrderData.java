package com.fa26se088.filmlink.dto.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//class dùng để trả dữ liệu mã đơn, phí đơn hàng từ API của GHN, chỉ lấy order_code và total_fee
@JsonIgnoreProperties(ignoreUnknown = true)
public record GhnOrderData(
        @JsonProperty("order_code")
        String orderCode,

        @JsonProperty("total_fee")
        Long totalFee
) {}
