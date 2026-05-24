package com.checkout.model;

import java.util.Set;

/**
 * Defines every valid state an Order can occupy and which transitions are legal.
 *
 *  CREATED → PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED
 *                          ↘                ↘
 *                      PAYMENT_FAILED    CANCELLED
 */
public enum OrderStatus {

    CREATED {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(PENDING_PAYMENT, CANCELLED);
        }
    },
    PENDING_PAYMENT {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(PAID, PAYMENT_FAILED, CANCELLED);
        }
    },
    PAID {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(PROCESSING, CANCELLED);
        }
    },
    PAYMENT_FAILED {
        @Override public Set<OrderStatus> validTransitions() {
            // Allow retry: go back to PENDING_PAYMENT
            return Set.of(PENDING_PAYMENT, CANCELLED);
        }
    },
    PROCESSING {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(); // terminal
        }
    },
    CANCELLED {
        @Override public Set<OrderStatus> validTransitions() {
            return Set.of(); // terminal
        }
    };

    public abstract Set<OrderStatus> validTransitions();

    public boolean canTransitionTo(OrderStatus next) {
        return validTransitions().contains(next);
    }
}
