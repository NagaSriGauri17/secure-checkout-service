package com.checkout.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String webhookUrl;
    private String statusDetail;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Order() {}

    private Order(Builder b) {
        this.id = b.id;
        this.customerId = b.customerId;
        this.totalAmount = b.totalAmount;
        this.currency = b.currency;
        this.status = b.status;
        this.webhookUrl = b.webhookUrl;
        this.statusDetail = b.statusDetail;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = "ord_" + UUID.randomUUID().toString().replace("-", "");
        if (this.status == null) this.status = OrderStatus.CREATED;
    }

    public void transitionTo(OrderStatus next) {
        if (!this.status.canTransitionTo(next))
            throw new IllegalStateException("Invalid order transition: " + this.status + " → " + next);
        this.status = next;
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public String getWebhookUrl() { return webhookUrl; }
    public String getStatusDetail() { return statusDetail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, customerId, currency, webhookUrl, statusDetail;
        private BigDecimal totalAmount;
        private OrderStatus status;

        public Builder id(String id) { this.id = id; return this; }
        public Builder customerId(String v) { this.customerId = v; return this; }
        public Builder totalAmount(BigDecimal v) { this.totalAmount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder status(OrderStatus v) { this.status = v; return this; }
        public Builder webhookUrl(String v) { this.webhookUrl = v; return this; }
        public Builder statusDetail(String v) { this.statusDetail = v; return this; }
        public Order build() { return new Order(this); }
    }
}
