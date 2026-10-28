package com.fa26se088.filmlink.service.delivery;

import com.fa26se088.filmlink.dto.delivery.Quote;
import com.fa26se088.filmlink.dto.delivery.TrackingEvent;
import com.fa26se088.filmlink.dto.delivery.request.ShipmentRequest;
import com.fa26se088.filmlink.dto.delivery.response.ShipmentResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DeliveryProvider {
    // Các khả năng của provider, ví dụ GHN có thể giao trong nội thành (Intra_city), liên tỉnh (Intercity) và thu hộ (COD)
    enum Capability { INTRA_CITY, INTERCITY, COD }

    /** Mã định danh duy nhất, ví dụ "ghn". Cũng là {provider} trong URL webhook. */
    String code();

    Set<Capability> capabilities();

    /** Báo giá. */
    List<Quote> quote(ShipmentRequest request);

    /**
     * Tạo vận đơn.
     * @param idempotencyKey khóa chống tạo trùng nếu provider hỗ trợ; nếu không thì adapter
     *                       dùng mã đơn nội bộ (client_order_code...) để đạt hiệu quả tương tự
     */
    ShipmentResult create(ShipmentRequest request, String idempotencyKey);

    void cancel(String providerOrderId);

    /**
     * Xác thực webhook (token/chữ ký) và chuyển payload của provider thành TrackingEvent chuẩn.
     * @param headers map header KHÔNG phân biệt hoa/thường
//     * @throws com.example.lab.exception.InvalidWebhookException nếu xác thực thất bại
     */
    TrackingEvent parseWebhook(String rawBody, Map<String, String> headers);
}
