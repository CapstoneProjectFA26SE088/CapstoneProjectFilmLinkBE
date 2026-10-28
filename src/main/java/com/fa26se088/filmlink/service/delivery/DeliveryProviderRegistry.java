package com.fa26se088.filmlink.service.delivery;

import com.fa26se088.filmlink.exception.UnknownProviderException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Class này là khi controller gọi xuống sẽ phải đi tìm provider hiện có trong hệ
// thống để kết nối gọi api provider đó để xử lý
@Component
public class DeliveryProviderRegistry {
    private final Map<String, DeliveryProvider> byCode;

    public DeliveryProviderRegistry(List<DeliveryProvider> providers) {
        // toUnmodifiableMap ném lỗi nếu hai provider trùng code → phát hiện sớm khi khởi động
        this.byCode = providers.stream()
                .collect(Collectors.toUnmodifiableMap(DeliveryProvider::code, Function.identity()));
    }

    public DeliveryProvider get(String code) {
        DeliveryProvider p = byCode.get(code);
        if (p == null) throw new UnknownProviderException(code);
        return p;
    }

    public Set<String> availableCodes() {
        return byCode.keySet();
    }
}
