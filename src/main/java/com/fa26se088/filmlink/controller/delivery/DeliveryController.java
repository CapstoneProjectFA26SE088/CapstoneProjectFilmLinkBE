package com.fa26se088.filmlink.controller.delivery;


import com.fa26se088.filmlink.dto.delivery.Quote;
import com.fa26se088.filmlink.dto.delivery.request.ShipmentRequest;
import com.fa26se088.filmlink.dto.delivery.response.ShipmentResponse;
import com.fa26se088.filmlink.service.delivery.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery", description = "Quản lý đơn hàng giao nhận ")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/quote")
    public List<Quote> quote(@RequestBody ShipmentRequest req) {
        return deliveryService.quoteAll(req);
    }

    /** ?provider=ghn để chọn; bỏ trống thì dùng delivery.default-provider. */
    @PostMapping
    public ShipmentResponse create(@RequestBody ShipmentRequest req,
                                   @RequestParam(required = false) String provider) {
        return ShipmentResponse.from(deliveryService.createShipment(req, provider));
    }

    @PostMapping("/{orderCode}/cancel")
    public ShipmentResponse cancel(@PathVariable String orderCode) {
        return ShipmentResponse.from(deliveryService.cancel(orderCode));
    }
}
