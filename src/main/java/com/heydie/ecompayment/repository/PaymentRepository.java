package com.heydie.ecompayment.repository;

import com.heydie.ecompayment.entity.Payment;
import com.heydie.ecompayment.entity.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByMidtransOrderId(String midtransOrderId);

    boolean existsByOrderId(String orderId); //idempotency check di step 5

    // dipakai PaymentReconciliationJob (step 9)
    List<Payment> findByStatusInAndCreatedDateBefore(Collection<Status> statuses, LocalDateTime cutOff);
}
