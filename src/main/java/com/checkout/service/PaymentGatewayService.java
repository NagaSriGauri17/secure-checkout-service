package com.checkout.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);
    private static final Random RANDOM = new Random();

    public record GatewayResult(boolean success, String transactionId, String errorMessage) {}

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "gatewayFallback")
    @Retry(name = "payment-gateway")
    public GatewayResult charge(String paymentMethodToken, BigDecimal amount, String currency) {
        log.info("Sending charge request: token={} amount={} {}", paymentMethodToken, amount, currency);
        simulateLatency();

        if (paymentMethodToken.startsWith("tok_fail")) {
            log.warn("Gateway hard decline for token={}", paymentMethodToken);
            return new GatewayResult(false, null, "Card declined by issuer");
        }

        int scenario = RANDOM.nextInt(10);
        if (scenario < 2) {
            log.warn("Simulated transient gateway error — will retry");
            throw new RuntimeException("Gateway timeout — transient error");
        }

        String txId = "gw_txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Gateway charge successful: transactionId={}", txId);
        return new GatewayResult(true, txId, null);
    }

    public GatewayResult gatewayFallback(String token, BigDecimal amount, String currency, Throwable ex) {
        log.error("Circuit breaker OPEN — gateway unavailable: {}", ex.getMessage());
        return new GatewayResult(false, null, "Payment gateway temporarily unavailable. Please retry later.");
    }

    private void simulateLatency() {
        try { Thread.sleep(100 + RANDOM.nextInt(200)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
