package com.fa26se088.filmlink.service.notification.event;

import com.fa26se088.filmlink.entity.notification.enums.NotificationType;

/**
 * Sự kiện yêu cầu gửi thông báo dùng chung cho các domain (UC04).
 *
 * <p>Domain khác có thể publish sự kiện này thông qua Spring {@link
 * org.springframework.context.ApplicationEventPublisher} thay vì gọi trực tiếp {@code
 * NotificationService} hoặc {@code EmailClient}, giúp tách biệt domain và đảm bảo ranh giới
 * transaction.
 *
 * @param accountId ID tài khoản người nhận
 * @param type loại thông báo
 * @param title tiêu đề thông báo
 * @param body nội dung thông báo
 * @param link đường dẫn liên kết hành động chi tiết (nếu có, có thể null)
 */
public record NotificationEvent(
    Long accountId,
    NotificationType type,
    String title,
    String body,
    String link) {}
