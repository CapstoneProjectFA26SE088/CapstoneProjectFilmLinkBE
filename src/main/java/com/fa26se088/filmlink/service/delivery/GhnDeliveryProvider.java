package com.fa26se088.filmlink.service.delivery;

import com.fa26se088.filmlink.client.delivery.GhnProperties;
import com.fa26se088.filmlink.dto.delivery.*;
import com.fa26se088.filmlink.dto.delivery.request.ShipmentRequest;
import com.fa26se088.filmlink.dto.delivery.response.ShipmentResult;
import com.fa26se088.filmlink.entity.delivery.enums.DeliveryStatus;
import com.fa26se088.filmlink.exception.DeliveryProviderException;
import com.fa26se088.filmlink.exception.InvalidWebhookException;
import com.fa26se088.filmlink.mapper.delivery.GhnStatusMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static jakarta.xml.bind.DatatypeConverter.parseTime;
import static org.springframework.http.RequestEntity.post;
// Gọi Ghn API
@Service
public class GhnDeliveryProvider implements DeliveryProvider{

    private static final int SERVICE_TYPE_ECOMMERCE = 2;
    // GHN có nhiều loại dịch vụ, 2 là e-commerce (giao hàng cho shop online) cho đơn hàng < 50kg

    private final RestClient http;
    private final GhnProperties props;
    private final GhnStatusMapper statusMapper;
    private final ObjectMapper mapper;

    public GhnDeliveryProvider(@Qualifier("ghnRestClient") RestClient http,
                               GhnProperties props,
                               GhnStatusMapper statusMapper,
                               ObjectMapper mapper) {
        this.http = http;
        this.props = props;
        this.statusMapper = statusMapper;
        this.mapper = mapper;
    }

    @Override
    public String code() {
        return "ghn";
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.of(Capability.INTRA_CITY, Capability.INTERCITY, Capability.COD);
    }

    @Override
    public List<Quote> quote(ShipmentRequest request) {
        Map<String, Object> body = Map.of(
                "service_type_id", SERVICE_TYPE_ECOMMERCE,
                "to_district_id", Integer.parseInt(request.receiver().districtCode()),
                "to_ward_code", request.receiver().wardCode(),
                "weight", request.weightGram(),
                "insurance_value", request.declaredValue());

        var res = post(
                "/shiip/public-api/v2/shipping-order/fee",
                body,
                new ParameterizedTypeReference<GhnEnvelope<GhnFeeData>>() {}
        );

        return List.of(
                new Quote(code(), "standard", res.data().total(), null)
        );
    }

    @Override
    public ShipmentResult create(ShipmentRequest request, String idempotencyKey) {
        // GHN dùng client_order_code (mã đơn nội bộ) làm mã tham chiếu phía bạn,
        // nên không cần tới idempotencyKey. Xác nhận với docs hành vi khi gửi trùng.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_order_code", request.orderCode());
        body.put("to_name", request.receiver().name());
        body.put("to_phone", request.receiver().phone());
        body.put("to_address", request.receiver().street());
        body.put("to_ward_code", request.receiver().wardCode());
        body.put("to_district_id", Integer.parseInt(request.receiver().districtCode()));
        body.put("weight", request.weightGram());
        body.put("cod_amount", request.codAmount());               // tiền thu hộ
        body.put("insurance_value", request.declaredValue());
        body.put("service_type_id", SERVICE_TYPE_ECOMMERCE);
        body.put("payment_type_id", 1);                      // người gửi trả phí ship
        body.put("required_note", "CHOXEMHANGKHONGTHU");
        body.put("note", request.note());

        var res = post(
                "/shiip/public-api/v2/shipping-order/create",
                body,
                new ParameterizedTypeReference<GhnEnvelope<GhnOrderData>>() {}
        );

        GhnOrderData d = res.data();

        long fee = d.totalFee() == null ? 0 : d.totalFee();

        return new ShipmentResult(
                d.orderCode(),
                d.orderCode(),
                fee,
                DeliveryStatus.CREATED
        );
    }

    @Override
    public void cancel(String providerOrderId) {
        post(
                "/shiip/public-api/v2/switch-status/cancel",
                Map.of("order_codes", List.of(providerOrderId)),
                new ParameterizedTypeReference<GhnEnvelope<Object>>() {}
        );
    }

    @Override
    public TrackingEvent parseWebhook(String rawBody, Map<String, String> headers) {
        // 1) Xác thực: so sánh token chia sẻ bằng hàm hằng-thời-gian (chống timing attack).
        //    Tùy cơ chế GHN hỗ trợ, có thể thay bằng secret trong URL + whitelist IP.
        String token = headers.getOrDefault("Token", "");
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                props.webhookToken().getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookException("Sai token webhook GHN");
        }

        // 2) Đọc payload GHN
        JsonNode n;
        try {
            n = mapper.readTree(rawBody);
        } catch (Exception e) {
            throw new InvalidWebhookException("Payload GHN không hợp lệ", e);
        }
        String orderCode = n.path("OrderCode").asText();
        String status = n.path("Status").asText();
        String time = n.path("Time").asText("");

        // 3) GHN không có sẵn event id → tự băm để làm khóa idempotency
        String eventId = sha256(orderCode + "|" + status + "|" + time);

        // 4) Chuẩn hóa sang TrackingEvent của hệ thống
        return new TrackingEvent(code(), eventId, orderCode,
                n.path("ClientOrderCode").asText(null),
                statusMapper.map(status),
                parseTime(time),
                rawBody);
    }

    private <T> GhnEnvelope<T> post(
            String path,
            Object body,
            ParameterizedTypeReference<GhnEnvelope<T>> type) {
        try {
            GhnEnvelope<T> res = http.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new DeliveryProviderException(
                                code(),
                                "GHN trả HTTP " + resp.getStatusCode()
                        );
                    })
                    .body(type);

            if (res == null || res.code() != 200) {
                throw new DeliveryProviderException(
                        code(),
                        "GHN từ chối: "
                                + (res == null
                                ? "phản hồi rỗng"
                                : res.message())
                );
            }

            return res;

        } catch (ResourceAccessException e) {
            // Timeout hoặc mất kết nối
            throw new DeliveryProviderException(
                    code(),
                    "Không kết nối được GHN",
                    e
            );
        }
    }

    private static Instant parseTime(String time) {
        try {
            return time.isBlank() ? Instant.now() : OffsetDateTime.parse(time).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
