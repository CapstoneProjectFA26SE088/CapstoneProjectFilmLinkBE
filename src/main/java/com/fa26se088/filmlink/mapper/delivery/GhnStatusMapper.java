package com.fa26se088.filmlink.mapper.delivery;

import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;
import org.springframework.stereotype.Component;

@Component
public class GhnStatusMapper {
    public DeliveryStatus map(String ghnStatus) {
        if (ghnStatus == null) return null;
        return switch (ghnStatus) {
            case "ready_to_pick" -> DeliveryStatus.CREATED;
            case "picking", "money_collect_picking" -> DeliveryStatus.PICKING;
            case "picked" -> DeliveryStatus.PICKED_UP;
            case "storing", "transporting", "sorting" -> DeliveryStatus.IN_TRANSIT;
            case "delivering", "money_collect_delivering" -> DeliveryStatus.DELIVERING;
            case "delivered" -> DeliveryStatus.DELIVERED;
            case "delivery_fail" -> DeliveryStatus.DELIVERY_FAILED;
            case "waiting_to_return", "return", "return_transporting", "return_sorting", "returning" ->
                    DeliveryStatus.RETURNING;
            case "returned" -> DeliveryStatus.RETURNED;
            case "cancel" -> DeliveryStatus.CANCELLED;
            default -> null;   // trạng thái lạ: bỏ qua thay vì làm lỗi cả webhook
        };
    }
}
