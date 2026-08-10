package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Payment;
import com.ltc.logisticsproject.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCargoIdOrderByCreatedAtDesc(Long cargoId);
    Optional<Payment> findFirstByCargoIdAndStatusOrderByCreatedAtDesc(Long cargoId, PaymentStatus status);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
