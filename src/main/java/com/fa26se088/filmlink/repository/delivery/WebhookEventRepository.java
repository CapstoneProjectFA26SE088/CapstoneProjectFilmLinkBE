package com.fa26se088.filmlink.repository.delivery;

import com.fa26se088.filmlink.entity.delivery.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByProviderAndEventId(String provider, String eventId);
}
