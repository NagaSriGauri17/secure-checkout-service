package com.checkout.dto;

import com.checkout.model.OrderStatus;
import com.checkout.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CheckoutDtos {

    // ── Create Order Request ──────────────────────────────────────────────────
    @Schema(description = "Request body to create a new order")
    public static class CreateOrderRequest {
        @NotBlank(message = "customerId must not be blank")
        public String customerId;
        @NotNull @Positive
        public BigDecimal totalAmount;
        @NotBlank @Size(min = 3, max = 3)
        public String currency;
        public String webhookUrl;
    }

    // ── Order Response ────────────────────────────────────────────────────────
    public static class OrderResponse {
        public String id;
        public String customerId;
        public BigDecimal totalAmount;
        public String currency;
        public OrderStatus status;
        public String statusDetail;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final OrderResponse r = new OrderResponse();
            public Builder id(String v) { r.id = v; return this; }
            public Builder customerId(String v) { r.customerId = v; return this; }
            public Builder totalAmount(BigDecimal v) { r.totalAmount = v; return this; }
            public Builder currency(String v) { r.currency = v; return this; }
            public Builder status(OrderStatus v) { r.status = v; return this; }
            public Builder statusDetail(String v) { r.statusDetail = v; return this; }
            public Builder createdAt(LocalDateTime v) { r.createdAt = v; return this; }
            public Builder updatedAt(LocalDateTime v) { r.updatedAt = v; return this; }
            public OrderResponse build() { return r; }
        }
    }

    // ── Payment Request ───────────────────────────────────────────────────────
    @Schema(description = "Payment initiation request")
    public static class PaymentRequest {
        @NotBlank
        @Schema(description = "Client-generated idempotency key — same key = same response, no double charge")
        public String idempotencyKey;
        @NotBlank
        public String paymentMethodToken;
    }

    // ── Payment Response ──────────────────────────────────────────────────────
    public static class PaymentResponse {
        public String paymentId;
        public String orderId;
        public PaymentStatus status;
        public String gatewayTransactionId;
        public LocalDateTime processedAt;
        public boolean idempotentReplay;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final PaymentResponse r = new PaymentResponse();
            public Builder paymentId(String v) { r.paymentId = v; return this; }
            public Builder orderId(String v) { r.orderId = v; return this; }
            public Builder status(PaymentStatus v) { r.status = v; return this; }
            public Builder gatewayTransactionId(String v) { r.gatewayTransactionId = v; return this; }
            public Builder processedAt(LocalDateTime v) { r.processedAt = v; return this; }
            public Builder idempotentReplay(boolean v) { r.idempotentReplay = v; return this; }
            public PaymentResponse build() { return r; }
        }
    }

    // ── Error Response ────────────────────────────────────────────────────────
    public static class ErrorResponse {
        public String code;
        public String message;
        public LocalDateTime timestamp;

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final ErrorResponse r = new ErrorResponse();
            public Builder code(String v) { r.code = v; return this; }
            public Builder message(String v) { r.message = v; return this; }
            public Builder timestamp(LocalDateTime v) { r.timestamp = v; return this; }
            public ErrorResponse build() { return r; }
        }
    }
}
