package com.fa26se088.filmlink.client.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "delivery.ghn",name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GhnProperties.class)
public class GhnClient {

    @Bean
    RestClient ghnRestClient(GhnProperties p){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        return RestClient.builder()
                .baseUrl(p.baseUrl())
                .defaultHeader("Token", p.token())
                .defaultHeader("ShopId", p.shopId())
                .defaultHeader("WebhookToken", p.webhookToken())
                .build();
    }
}
