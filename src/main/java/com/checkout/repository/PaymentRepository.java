package com.checkout.repository;

import com.checkout.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** Used to implement idempotency — return existing record if key already seen. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(String orderId);
}
