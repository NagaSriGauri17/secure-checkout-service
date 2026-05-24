package com.checkout;

import com.checkout.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderStatus state machine")
class OrderStatusStateMachineTest {

    @ParameterizedTest(name = "{0} → {1} should be valid")
    @CsvSource({
        "CREATED,           PENDING_PAYMENT",
        "CREATED,           CANCELLED",
        "PENDING_PAYMENT,   PAID",
        "PENDING_PAYMENT,   PAYMENT_FAILED",
        "PENDING_PAYMENT,   CANCELLED",
        "PAYMENT_FAILED,    PENDING_PAYMENT",   // retry path
        "PAID,              PROCESSING",
        "PAID,              CANCELLED",
        "PROCESSING,        SHIPPED",
        "SHIPPED,           DELIVERED",
    })
    void validTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to))
            .as("Expected %s → %s to be valid", from, to)
            .isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} should be INVALID")
    @CsvSource({
        "CREATED,       PAID",
        "PAID,          CREATED",
        "DELIVERED,     SHIPPED",
        "CANCELLED,     PAID",
        "DELIVERED,     PROCESSING",
    })
    void invalidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to))
            .as("Expected %s → %s to be invalid", from, to)
            .isFalse();
    }

    @Test
    @DisplayName("Terminal states have no valid transitions")
    void terminalStatesAreTerminal() {
        assertThat(OrderStatus.DELIVERED.validTransitions()).isEmpty();
        assertThat(OrderStatus.CANCELLED.validTransitions()).isEmpty();
    }
}
