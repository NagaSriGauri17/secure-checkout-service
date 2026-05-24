package com.checkout.service;

import com.checkout.dto.CheckoutDtos.*;
import com.checkout.exception.CheckoutException;
import com.checkout.model.*;
import com.checkout.repository.OrderRepository;
import com.checkout.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService gatewayService;
    private final WebhookService webhookService;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        PaymentGatewayService gatewayService,
                        WebhookService webhookService,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.gatewayService = gatewayService;
        this.webhookService = webhookService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        MDC.put("correlationId", UUID.randomUUID().toString());
        log.info("Creating order for customer={} amount={} {}", req.customerId, req.totalAmount, req.currency);

        Order order = Order.builder()
            .customerId(req.customerId)
            .totalAmount(req.totalAmount)
            .currency(req.currency)
            .webhookUrl(req.webhookUrl)
            .status(OrderStatus.CREATED)
            .build();

        order = orderRepository.save(order);
        meterRegistry.counter("checkout.orders.created").increment();
        log.info("Order created: orderId={}", order.getId());
        MDC.clear();
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return toOrderResponse(findOrderOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
            .stream().map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse processPayment(String orderId, PaymentRequest req) {
        MDC.put("correlationId", UUID.randomUUID().toString());
        MDC.put("orderId", orderId);
        Timer.Sample sample = Timer.start(meterRegistry);

        // Idempotency check
        return paymentRepository.findByIdempotencyKey(req.idempotencyKey)
            .map(existing -> {
                log.info("Idempotency hit — returning cached payment: paymentId={}", existing.getId());
                meterRegistry.counter("checkout.payments.idempotent_replays").increment();
                return toPaymentResponse(existing, true);
            })
            .orElseGet(() -> executePayment(orderId, req, sample));
    }

    private PaymentResponse executePayment(String orderId, PaymentRequest req, Timer.Sample sample) {
        Order order = findOrderOrThrow(orderId);

        if (!order.getStatus().canTransitionTo(OrderStatus.PENDING_PAYMENT)) {
            throw new CheckoutException("ORDER_NOT_PAYABLE",
                "Order " + orderId + " in status " + order.getStatus() + " cannot accept payment");
        }

        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        Payment payment = Payment.builder()
            .orderId(orderId)
            .idempotencyKey(req.idempotencyKey)
            .amount(order.getTotalAmount())
            .currency(order.getCurrency())
            .status(PaymentStatus.PENDING)
            .build();
        payment = paymentRepository.save(payment);

        PaymentGatewayService.GatewayResult result =
            gatewayService.charge(req.paymentMethodToken, order.getTotalAmount(), order.getCurrency());

        payment.setGatewayResponse(result.errorMessage() != null ? result.errorMessage() : "OK");
        payment.setGatewayTransactionId(result.transactionId());
        payment.setProcessedAt(LocalDateTime.now());

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            order.transitionTo(OrderStatus.PAID);
            order.setStatusDetail("Payment processed: " + result.transactionId());
            meterRegistry.counter("checkout.payments.success").increment();
            log.info("Payment succeeded: paymentId={} txId={}", payment.getId(), result.transactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            order.setStatusDetail("Payment failed: " + result.errorMessage());
            meterRegistry.counter("checkout.payments.failed").increment();
            log.warn("Payment failed: paymentId={} reason={}", payment.getId(), result.errorMessage());
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        if (order.getWebhookUrl() != null) {
            webhookService.enqueue(order, payment);
        }

        sample.stop(meterRegistry.timer("checkout.payment.latency"));
        MDC.clear();
        return toPaymentResponse(payment, false);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = findOrderOrThrow(orderId);
        log.info("Transitioning order {} from {} to {}", orderId, order.getStatus(), newStatus);
        order.transitionTo(newStatus);
        orderRepository.save(order);
        return toOrderResponse(order);
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CheckoutException("ORDER_NOT_FOUND", "Order not found: " + orderId));
    }

    private OrderResponse toOrderResponse(Order o) {
        return OrderResponse.builder()
            .id(o.getId())
            .customerId(o.getCustomerId())
            .totalAmount(o.getTotalAmount())
            .currency(o.getCurrency())
            .status(o.getStatus())
            .statusDetail(o.getStatusDetail())
            .createdAt(o.getCreatedAt())
            .updatedAt(o.getUpdatedAt())
            .build();
    }

    private PaymentResponse toPaymentResponse(Payment p, boolean replay) {
        return PaymentResponse.builder()
            .paymentId(p.getId())
            .orderId(p.getOrderId())
            .status(p.getStatus())
            .gatewayTransactionId(p.getGatewayTransactionId())
            .processedAt(p.getProcessedAt())
            .idempotentReplay(replay)
            .build();
    }
}
