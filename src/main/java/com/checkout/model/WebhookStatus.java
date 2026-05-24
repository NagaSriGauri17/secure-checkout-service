package com.checkout.model;

public enum WebhookStatus {
    PENDING,
    DELIVERED,
    FAILED,
    EXHAUSTED   // max retries exceeded
}
