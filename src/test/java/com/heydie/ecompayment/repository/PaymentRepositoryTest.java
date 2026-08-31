package com.heydie.ecompayment.repository;

import com.heydie.ecompayment.entity.Payment;
import com.heydie.ecompayment.entity.enumeration.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PaymentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    PaymentRepository repository;

    @Test
    void findByOrderId_and_findByMidtransOrderId_returnSavedPayment() {
        repository.saveAndFlush(newPayment("ORD-1", "ORD-1-1"));

        assertThat(repository.findByOrderId("ORD-1")).isPresent();
        assertThat(repository.findByMidtransOrderId("ORD-1-1")).isPresent();
        assertThat(repository.findByOrderId("ORD-404")).isEmpty();
    }

    @Test
    void existsByOrderId_reflectsPresence() {
        repository.saveAndFlush(newPayment("ORD-2", "ORD-2-1"));

        assertThat(repository.existsByOrderId("ORD-2")).isTrue();
        assertThat(repository.existsByOrderId("ORD-404")).isFalse();
    }

    private static Payment newPayment(String orderId, String midtransOrderId) {
        return Payment.builder()
                .orderId(orderId)
                .midtransOrderId(midtransOrderId)
                .grossAmount(BigInteger.valueOf(250_000))
                .currency("IDR")
                .status(Status.PENDING)
                .attempt(1)                 // kolom NOT NULL; entity Integer attempt tanpa default
                .build();
        // version: dibiarkan null → Hibernate @Version isi 0 saat persist
        // created_date/created_by/...: diisi @PrePersist di BaseEntity
    }
}
