package com.fa26se088.filmlink.controller.delivery;

import com.fa26se088.filmlink.exception.InvalidWebhookException;
import com.fa26se088.filmlink.exception.UnknownProviderException;
import com.fa26se088.filmlink.service.delivery.DeliveryWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/delivery")
@Tag(name = "Webhook", description = "Đăng ký, đăng nhập và quản lý phiên bản webhook của các nhà cung cấp dịch vụ giao nhận")
public class DeliveryWebhookController {
    private final DeliveryWebhookService webhookService;

    public DeliveryWebhookController(DeliveryWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> handle(@PathVariable String provider,
                                       @RequestBody String rawBody,   // giữ nguyên body thô để xác thực chữ ký
                                       @RequestHeader HttpHeaders headers) {
        // toSingleValueMap cho map không phân biệt hoa/thường
        webhookService.process(provider, rawBody, headers.toSingleValueMap());
        return ResponseEntity.ok().build();   // trả 200 nhanh; việc nặng nên đẩy sang queue/async
    }

    @ExceptionHandler(InvalidWebhookException.class)
    ResponseEntity<Void> invalid() {
        return ResponseEntity.status(401).build();
    }

    @ExceptionHandler(UnknownProviderException.class)
    ResponseEntity<Void> unknown() {
        return ResponseEntity.notFound().build();
    }
}
