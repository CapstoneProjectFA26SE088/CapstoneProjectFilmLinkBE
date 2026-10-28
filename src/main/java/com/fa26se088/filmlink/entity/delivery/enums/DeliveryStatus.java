package com.fa26se088.filmlink.entity.delivery.enums;

public enum DeliveryStatus {
    CREATED(0),          // vừa tạo đơn
    PICKING(1),          // đang đi lấy hàng
    PICKED_UP(2),        // đã lấy hàng
    IN_TRANSIT(3),       // đang trung chuyển / lưu kho
    DELIVERING(4),       // đang giao cho khách
    DELIVERY_FAILED(4),  // giao thất bại (có thể giao lại nên cùng rank với DELIVERING)
    RETURNING(5),        // đang hoàn hàng
    RETURNED(6),         // đã hoàn xong      (kết thúc)
    DELIVERED(6),        // giao thành công   (kết thúc)
    CANCELLED(7);        // đã hủy            (kết thúc)


    /** Thứ tự "tiến" của trạng thái, dùng để chặn event đến sai thứ tự. */
    private final int rank;

    DeliveryStatus(int rank) {
        this.rank = rank;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }

    /** Chỉ cho phép đi tiến (hoặc giữ nguyên); đã kết thúc thì không đổi nữa. */
    public boolean canTransitionTo(DeliveryStatus next) {
        if (next == null || isTerminal()) return false;
        return next.rank >= this.rank;
    }
}
