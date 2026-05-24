package com.checkout.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "webhook_events",
    indexes = {
        @Index(name = "idx_webhook_status", columnList = "status"),
        @Index(name = "idx_webhook_order",  columnList = "orderId")
    }
)
public class WebhookEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;

    private int attemptCount;
    private LocalDateTime nextRetryAt;
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    public WebhookEvent() {}

    private WebhookEvent(Builder b) {
        this.id = b.id;
        this.orderId = b.orderId;
        this.targetUrl = b.targetUrl;
        this.payload = b.payload;
        this.status = b.status;
        this.attemptCount = b.attemptCount;
        this.nextRetryAt = b.nextRetryAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = "wh_" + UUID.randomUUID().toString().replace("-", "");
        if (this.status == null) this.status = WebhookStatus.PENDING;
    }

    // Getters
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getTargetUrl() { return targetUrl; }
    public String getPayload() { return payload; }
    public WebhookStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setStatus(WebhookStatus status) { this.status = status; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, orderId, targetUrl, payload;
        private WebhookStatus status;
        private int attemptCount;
        private LocalDateTime nextRetryAt;

        public Builder id(String v) { this.id = v; return this; }
        public Builder orderId(String v) { this.orderId = v; return this; }
        public Builder targetUrl(String v) { this.targetUrl = v; return this; }
        public Builder payload(String v) { this.payload = v; return this; }
        public Builder status(WebhookStatus v) { this.status = v; return this; }
        public Builder attemptCount(int v) { this.attemptCount = v; return this; }
        public Builder nextRetryAt(LocalDateTime v) { this.nextRetryAt = v; return this; }
        public WebhookEvent build() { return new WebhookEvent(this); }
    }
}
