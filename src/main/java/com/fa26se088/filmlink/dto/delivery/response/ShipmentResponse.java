package com.fa26se088.filmlink.dto.delivery.response;

import com.fa26se088.filmlink.entity.delivery.Shipment;
import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;

public record ShipmentResponse (
        String orderCode,
        String provider,
        String providerOrderId,
        DeliveryStatus status,
        long fee,
        long codAmount) {

    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(s.getOrderCode(), s.getProvider(), s.getProviderOrderId(),
                s.getStatus(), s.getFee(), s.getCodAmount());
    }
}

