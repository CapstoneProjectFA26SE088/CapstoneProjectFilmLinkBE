package com.fa26se088.filmlink.dto.delivery;

import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;

import java.time.Instant;

public record TrackingEvent(
        String providerCode,
        String eventId,
        String providerOrderId,
        String clientOrderCode,   // mã đơn nội bộ nếu provider trả lại, có thể null
        DeliveryStatus status,    // null nếu là trạng thái ta không quan tâm
        Instant occurredAt,
        String rawPayload
) {
}
