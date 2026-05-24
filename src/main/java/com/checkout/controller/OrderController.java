package com.checkout.controller;

import com.checkout.dto.CheckoutDtos.*;
import com.checkout.model.OrderStatus;
import com.checkout.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order creation and lifecycle management")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/v1/orders — customer={}", request.customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping
    @Operation(summary = "List orders by customer")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@RequestParam String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Advance order status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Initiate payment — idempotent, same key = same response, no double charge")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRequest request) {
        log.info("POST /api/v1/orders/{}/pay idempotencyKey={}", orderId, request.idempotencyKey);
        return ResponseEntity.ok(orderService.processPayment(orderId, request));
    }
}
