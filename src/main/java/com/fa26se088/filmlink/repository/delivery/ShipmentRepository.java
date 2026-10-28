package com.fa26se088.filmlink.repository.delivery;

import com.fa26se088.filmlink.entity.delivery.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderCode(String orderCode);

    /** Dùng khi nhận webhook: tìm vận đơn theo mã của nhà vận chuyển. */
    Optional<Shipment> findByProviderAndProviderOrderId(String provider, String providerOrderId);
}
