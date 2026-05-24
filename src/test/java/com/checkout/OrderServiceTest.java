package com.checkout;

import com.checkout.dto.CheckoutDtos.*;
import com.checkout.model.*;
import com.checkout.repository.OrderRepository;
import com.checkout.repository.PaymentRepository;
import com.checkout.service.OrderService;
import com.checkout.service.PaymentGatewayService;
import com.checkout.service.WebhookService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — idempotency and payment flow")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGatewayService gatewayService;
    @Mock WebhookService webhookService;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository, paymentRepository,
            gatewayService, webhookService,
            new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("Idempotency: second payment request with same key returns cached response")
    void idempotentPayment_returnsCachedResponse() {
        Payment existingPayment = Payment.builder()
            .id("pay_existing")
            .orderId("ord_1")
            .idempotencyKey("idem_abc")
            .status(PaymentStatus.SUCCESS)
            .build();

        when(paymentRepository.findByIdempotencyKey("idem_abc"))
            .thenReturn(Optional.of(existingPayment));

        PaymentRequest req = new PaymentRequest();
        req.idempotencyKey = "idem_abc";
        req.paymentMethodToken = "tok_any";

        PaymentResponse response = orderService.processPayment("ord_1", req);

        assertThat(response.idempotentReplay).isTrue();
        assertThat(response.paymentId).isEqualTo("pay_existing");
        assertThat(response.status).isEqualTo(PaymentStatus.SUCCESS);
        verifyNoInteractions(gatewayService);
    }

    @Test
    @DisplayName("State machine: rejects payment on a DELIVERED order")
    void paymentOnDeliveredOrder_throws() {
        Order deliveredOrder = Order.builder()
            .id("ord_done")
            .status(OrderStatus.DELIVERED)
            .totalAmount(BigDecimal.TEN)
            .currency("INR")
            .build();

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepository.findById("ord_done")).thenReturn(Optional.of(deliveredOrder));

        PaymentRequest req = new PaymentRequest();
        req.idempotencyKey = "idem_new";
        req.paymentMethodToken = "tok_visa";

        assertThatThrownBy(() -> orderService.processPayment("ord_done", req))
            .hasMessageContaining("cannot accept payment");
    }
}
