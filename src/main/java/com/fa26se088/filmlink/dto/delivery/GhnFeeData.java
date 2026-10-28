package com.fa26se088.filmlink.dto.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
// class dùng để trả dữ liệu thông tin phí giao hàng của ghn
public record GhnFeeData(
        long total
){}
