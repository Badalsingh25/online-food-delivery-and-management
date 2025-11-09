package com.hungerexpress.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByProviderOrderId(String providerOrderId);
    Optional<PaymentEntity> findTopByOrder_IdOrderByCreatedAtDesc(Long orderId);
}
