package com.checkout.repository;

import com.checkout.model.WebhookEvent;
import com.checkout.model.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {

    /** Returns events due for delivery/retry. */
    List<WebhookEvent> findByStatusAndNextRetryAtBefore(WebhookStatus status, LocalDateTime now);

    List<WebhookEvent> findByOrderId(String orderId);
}
