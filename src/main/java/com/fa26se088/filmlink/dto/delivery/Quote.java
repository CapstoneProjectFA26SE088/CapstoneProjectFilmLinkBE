package com.fa26se088.filmlink.dto.delivery;

// một phiếu báo giá theo cấu trúc chung của hệ thống.
public record Quote(
        String providerCode,
        String serviceCode,   // tên/mã gói dịch vụ
        long fee,             // VND
        Integer etaHours) {   // thời gian dự kiến, null nếu provider không trả
}
