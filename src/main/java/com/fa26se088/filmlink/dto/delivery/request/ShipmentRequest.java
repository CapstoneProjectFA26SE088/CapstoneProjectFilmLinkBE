package com.fa26se088.filmlink.dto.delivery.request;

import com.fa26se088.filmlink.dto.delivery.ContactAddress;

public record ShipmentRequest(
        String orderCode,          // mã đơn nội bộ, cũng dùng làm khóa chống trùng
        ContactAddress sender,
        ContactAddress receiver,
        int weightGram,
        long codAmount,            // 0 nếu không thu hộ
        long declaredValue,        // giá trị khai báo (bảo hiểm)
        String note) {
}
