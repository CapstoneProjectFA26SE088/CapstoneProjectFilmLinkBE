package com.fa26se088.filmlink.service.delivery;

import com.fa26se088.filmlink.dto.delivery.TrackingEvent;
import com.fa26se088.filmlink.entity.delivery.Shipment;
import com.fa26se088.filmlink.entity.delivery.WebhookEvent;
import com.fa26se088.filmlink.repository.delivery.ShipmentRepository;
import com.fa26se088.filmlink.repository.delivery.WebhookEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

//Xử lý webhook trạng thái từ provider
@Service
public class DeliveryWebhookService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryWebhookService.class);

    private final DeliveryProviderRegistry registry;
    private final ShipmentRepository shipmentRepo;
    private final WebhookEventRepository eventRepo;

    public DeliveryWebhookService(DeliveryProviderRegistry registry,
                                  ShipmentRepository shipmentRepo,
                                  WebhookEventRepository eventRepo) {
        this.registry = registry;
        this.shipmentRepo = shipmentRepo;
        this.eventRepo = eventRepo;
    }

    @Transactional
    public void process(String providerCode, String rawBody, Map<String, String> headers) {
        DeliveryProvider provider = registry.get(providerCode);

        // 1) Xác thực + chuẩn hóa (ném InvalidWebhookException nếu sai token/chữ ký) của provider
        TrackingEvent ev = provider.parseWebhook(rawBody, headers);

        // 2) Chống xử lý trùng. Race condition hiếm gặp giữa hai request cùng event sẽ bị
        //    UNIQUE(provider, event_id) chặn; request châm hơn hoặc bị lỗi và provider gửi lại,
        if (eventRepo.existsByProviderAndEventId(providerCode, ev.eventId())) {
            log.info("Webhook trùng, bỏ qua: "
                    + providerCode + " " + ev.eventId());
            return;
        }
        eventRepo.save(new WebhookEvent(providerCode, ev.eventId()));

        // 3) Trạng thái ta không quan tâm (adapter trả null) thì bỏ qua nhưng vẫn trả 200 chỉ xem DeliveryStatus
        if (ev.status() == null) return;

        // 4) Tìm vận đơn. Không thấy: log rồi trả 200 để provider không gửi lại vô hạn
        Shipment shipment = shipmentRepo
                .findByProviderAndProviderOrderId(providerCode, ev.providerOrderId())
                .orElse(null);
        if (shipment == null) {
            log.warn("Không tìm thấy vận đơn "
                    + ev.providerOrderId() + " của " + providerCode);
            return;
        }

        // 5) Áp dụng nếu hợp lệ (đi tiến, không phải event cũ). Cập nhật trạng thái vận đơn
        boolean changed = shipment.applyStatus(ev.status(), ev.occurredAt());
        if (changed) {
            log.info("Đơn {} chuyển sang {}", shipment.getOrderCode(), ev.status());
            // TODO: tại đây phát sự kiện nội bộ (ApplicationEventPublisher) để gửi email/thông báo,
            //       cập nhật đơn hàng, ghi nhận COD khi DELIVERED...
        }
    }
}
