package com.fa26se088.filmlink.service.delivery;

import com.fa26se088.filmlink.dto.delivery.Quote;
import com.fa26se088.filmlink.dto.delivery.request.ShipmentRequest;
import com.fa26se088.filmlink.dto.delivery.response.ShipmentResult;
import com.fa26se088.filmlink.entity.delivery.Shipment;
import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;
import com.fa26se088.filmlink.exception.DeliveryProviderException;
import com.fa26se088.filmlink.repository.delivery.ShipmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Nghiệp vụ giao vận. Chỉ làm việc với interface DeliveryProvider
@Service
public class DeliveryService {

    private final DeliveryProviderRegistry registry;
    private final ShipmentRepository shipmentRepo;
    private final String defaultProvider;

    public DeliveryService(DeliveryProviderRegistry registry,
                           ShipmentRepository shipmentRepo,
                           @Value("${delivery.default-provider:ghn}") String defaultProvider) {
        this.registry = registry;
        this.shipmentRepo = shipmentRepo;
        this.defaultProvider = defaultProvider;
    }

    // Check giá provider trong hệ thống rồi ra list Quote (báo giá)
    public List<Quote> quoteAll(ShipmentRequest req) {
        List<Quote> result = new ArrayList<>();
        for (String code : registry.availableCodes()) {
            DeliveryProvider p = registry.get(code);
            if (req.codAmount() > 0 && !p.capabilities().contains(DeliveryProvider.Capability.COD)) continue;
            try {
                result.addAll(p.quote(req));
            } catch (DeliveryProviderException ignored) {
                // TODO: log cảnh báo; một provider chết không được kéo theo cả hệ thống
            }
        }
        return result;
    }

    //Tạo vận đơn.
    public Shipment createShipment(ShipmentRequest req, String providerCode) {
        double codAmount = req.codAmount();
        // 1) Kiểm tra xem đơn này đã có vận đơn chưa nếu đã tạo thì cho đơn này trả lại luôn
        Optional<Shipment> existing = shipmentRepo.findByOrderCode(req.orderCode());
        if (existing.isPresent()) return existing.get();

        String code = (providerCode == null || providerCode.isBlank()) ? defaultProvider : providerCode;
        DeliveryProvider provider = registry.get(code);

        // 2) Kiểm tra provider có hỗ trợ COD không

        if (codAmount < 0) {
            throw new IllegalArgumentException("COD không được âm");
        }

        if (req.codAmount() > 0 && !provider.capabilities().contains(DeliveryProvider.Capability.COD)) {
            throw new DeliveryProviderException(code, "Provider không hỗ trợ COD");
        }

        if (req.codAmount() > 0 && req.codAmount() < 50000) {
            throw new DeliveryProviderException(code, "COD phải >= 50.000 VND");
        }

        if (req.codAmount() > 2000000) {
            throw new DeliveryProviderException(code, "COD phải <= 2.000.000 VND");
        }

        // 3) Gọi provider. Khóa ổn định theo đơn hàng để retry không tạo vận đơn thứ hai
        ShipmentResult result = provider.create(req, "ship-" + req.orderCode());

        // 4) Lưu DB. Nếu hai request chạy song song, UNIQUE(order_code) sẽ chặn bản ghi thứ hai
        return shipmentRepo.save(Shipment.of(req, code, result));
    }

    public Shipment cancel(String orderCode) {
        Shipment s = shipmentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không có vận đơn cho đơn " + orderCode));
        registry.get(s.getProvider()).cancel(s.getProviderOrderId());
        s.applyStatus(DeliveryStatus.CANCELLED, Instant.now());
        return shipmentRepo.save(s);
    }

}
