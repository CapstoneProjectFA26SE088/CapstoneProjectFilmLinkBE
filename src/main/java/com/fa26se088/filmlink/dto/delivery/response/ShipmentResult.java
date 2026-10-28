package com.fa26se088.filmlink.dto.delivery.response;

import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;

public record ShipmentResult(
        String providerOrderId,
        String trackingCode,
        long fee,
        DeliveryStatus status
) {
}
