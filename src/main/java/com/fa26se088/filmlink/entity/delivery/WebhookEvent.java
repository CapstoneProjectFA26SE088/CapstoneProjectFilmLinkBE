package com.fa26se088.filmlink.entity.delivery;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

//Nhật ký webhook đã xử lý, phục vụ IDEMPOTENCY
@Entity
@Table(name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "event_id"}))
@Data
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    private Instant receivedAt = Instant.now();

    protected WebhookEvent() { }

    public WebhookEvent(String provider, String eventId) {
        this.provider = provider;
        this.eventId = eventId;
    }
}
