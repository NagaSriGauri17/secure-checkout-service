package com.checkout.service;

import com.checkout.model.*;
import com.checkout.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final WebhookEventRepository webhookEventRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${webhook.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${webhook.retry.backoff-ms:2000}")
    private long backoffMs;

    public WebhookService(WebhookEventRepository webhookEventRepository,
                          WebClient.Builder webClientBuilder) {
        this.webhookEventRepository = webhookEventRepository;
        this.webClientBuilder = webClientBuilder;
    }

    @Transactional
    public void enqueue(Order order, Payment payment) {
        String payload = buildPayload(order, payment);
        WebhookEvent event = WebhookEvent.builder()
            .orderId(order.getId())
            .targetUrl(order.getWebhookUrl())
            .payload(payload)
            .status(WebhookStatus.PENDING)
            .attemptCount(0)
            .nextRetryAt(LocalDateTime.now())
            .build();
        webhookEventRepository.save(event);
        log.info("Webhook enqueued: eventId={} orderId={} url={}",
            event.getId(), order.getId(), order.getWebhookUrl());
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void processPendingWebhooks() {
        var due = webhookEventRepository.findByStatusAndNextRetryAtBefore(
            WebhookStatus.PENDING, LocalDateTime.now());
        if (!due.isEmpty()) log.info("Processing {} pending webhook(s)", due.size());
        due.forEach(this::deliver);
    }

    @Async
    protected void deliver(WebhookEvent event) {
        log.info("Delivering webhook eventId={} attempt={} url={}",
            event.getId(), event.getAttemptCount() + 1, event.getTargetUrl());
        try {
            webClientBuilder.build()
                .post()
                .uri(event.getTargetUrl())
                .header("Content-Type", "application/json")
                .header("X-Checkout-Event-Id", event.getId())
                .header("X-Checkout-Order-Id", event.getOrderId())
                .bodyValue(event.getPayload())
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));

            event.setStatus(WebhookStatus.DELIVERED);
            event.setDeliveredAt(LocalDateTime.now());
            event.setLastError(null);
            log.info("Webhook delivered: eventId={}", event.getId());

        } catch (Exception ex) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(ex.getMessage());
            log.warn("Webhook delivery failed: eventId={} attempt={} error={}",
                event.getId(), attempts, ex.getMessage());

            if (attempts >= maxAttempts) {
                event.setStatus(WebhookStatus.EXHAUSTED);
                log.error("Webhook exhausted after {} attempts: eventId={}", maxAttempts, event.getId());
            } else {
                long delay = backoffMs * (long) Math.pow(2, attempts - 1);
                event.setNextRetryAt(LocalDateTime.now().plusNanos(delay * 1_000_000));
                event.setStatus(WebhookStatus.PENDING);
                log.info("Webhook retry scheduled in {}ms: eventId={}", delay, event.getId());
            }
        }
        webhookEventRepository.save(event);
    }

    private String buildPayload(Order order, Payment payment) {
        try {
            Map<String, Object> body = Map.of(
                "event",     "payment." + payment.getStatus().name().toLowerCase(),
                "orderId",   order.getId(),
                "paymentId", payment.getId(),
                "status",    order.getStatus().name(),
                "amount",    order.getTotalAmount(),
                "currency",  order.getCurrency(),
                "timestamp", LocalDateTime.now().toString()
            );
            return MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload", e);
            return "{}";
        }
    }
}
