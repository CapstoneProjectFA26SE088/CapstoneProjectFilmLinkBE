package com.fa26se088.filmlink.client.delivery;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.ghn")
public record GhnProperties (
        boolean enabled,
        String baseUrl,        // sandbox và production khác nhau → đổi bằng cấu hình, không sửa code
        String token,
        String shopId,
        String webhookToken  // secret dùng xác thực webhook (xem GhnDeliveryProvider.parseWebhook)
){}