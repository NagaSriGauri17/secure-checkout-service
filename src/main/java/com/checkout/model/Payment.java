package com.checkout.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_payments_order_id", columnList = "orderId")
    }
)
public class Payment {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;

    private String gatewayTransactionId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public Payment() {}

    private Payment(Builder b) {
        this.id = b.id;
        this.orderId = b.orderId;
        this.idempotencyKey = b.idempotencyKey;
        this.amount = b.amount;
        this.currency = b.currency;
        this.status = b.status;
        this.gatewayResponse = b.gatewayResponse;
        this.gatewayTransactionId = b.gatewayTransactionId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = "pay_" + UUID.randomUUID().toString().replace("-", "");
    }

    // Getters
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getGatewayResponse() { return gatewayResponse; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, orderId, idempotencyKey, currency, gatewayResponse, gatewayTransactionId;
        private BigDecimal amount;
        private PaymentStatus status;

        public Builder id(String v) { this.id = v; return this; }
        public Builder orderId(String v) { this.orderId = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }
        public Builder amount(BigDecimal v) { this.amount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder status(PaymentStatus v) { this.status = v; return this; }
        public Builder gatewayResponse(String v) { this.gatewayResponse = v; return this; }
        public Builder gatewayTransactionId(String v) { this.gatewayTransactionId = v; return this; }
        public Payment build() { return new Payment(this); }
    }
}
