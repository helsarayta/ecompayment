package com.heydie.ecompayment.payment.service;

import com.heydie.ecompayment.config.KafkaTopicProperties;
import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.entity.Payment;
import com.heydie.ecompayment.entity.enumeration.Status;
import com.heydie.ecompayment.midtrans.MidtransClient;
import com.heydie.ecompayment.midtrans.MidtransException;
import com.heydie.ecompayment.midtrans.dto.SnapChargeRequest;
import com.heydie.ecompayment.midtrans.dto.SnapChargeResponse;
import com.heydie.ecompayment.outbox.OutboxService;
import com.heydie.ecompayment.payment.messaging.dto.PaymentCreatedEvent;
import com.heydie.ecompayment.payment.messaging.dto.PaymentRequestedEvent;
import com.heydie.ecompayment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock MidtransClient midtransClient;
    @Mock OutboxService outboxService;

    // record asli — bukan mock
    private final MidtransProperties midtransProps = new MidtransProperties(
            false, "SB-Mid-server-test",
            "https://app.sandbox.midtrans.com", "https://api.sandbox.midtrans.com",
            Duration.ofSeconds(5), Duration.ofSeconds(15), null);
    private final KafkaTopicProperties topics =
            new KafkaTopicProperties("payment.request.v1", "payment.created.v1", "payment.result.v1");

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, midtransClient, midtransProps, outboxService, topics);
    }

    private static PaymentRequestedEvent event(String currency, Integer expiryMinutes) {
        return new PaymentRequestedEvent("evt-1", "ORD-1", BigInteger.valueOf(250_000), currency, expiryMinutes);
    }

    private void stubSaveAssignsId() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(99L);
            return p;
        });
    }

    // ---------------------------------------------------------------- happy path

    @Test
    void initiate_createsSnap_persistsPending_andWritesOutbox() {
        when(paymentRepository.existsByOrderId("ORD-1")).thenReturn(false);
        stubSaveAssignsId();
        when(midtransClient.createSnapTransaction(any())).thenReturn(
                new SnapChargeResponse("tok-1", "https://app.sandbox.midtrans.com/snap/v2/vtweb/tok-1"));

        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
        service.initiate(event("IDR", 30));

        // Snap request: midtransOrderId "ORD-1-1", amount 250000, expiry 30
        ArgumentCaptor<SnapChargeRequest> snapReq = ArgumentCaptor.forClass(SnapChargeRequest.class);
        verify(midtransClient).createSnapTransaction(snapReq.capture());
        SnapChargeRequest req = snapReq.getValue();
        assertThat(req.transactionDetails().orderId()).isEqualTo("ORD-1-1");
        assertThat(req.transactionDetails().grossAmount()).isEqualTo(250_000L);
        assertThat(req.expiry().duration()).isEqualTo(30);

        // Payment tersimpan: 2x save; state akhir = PENDING + token
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(saved.capture());
        Payment last = saved.getValue();
        assertThat(last.getMidtransOrderId()).isEqualTo("ORD-1-1");
        assertThat(last.getAttempt()).isEqualTo(1);
        assertThat(last.getGrossAmount()).isEqualTo(BigInteger.valueOf(250_000));
        assertThat(last.getStatus()).isEqualTo(Status.PENDING);
        assertThat(last.getSnapToken()).isEqualTo("tok-1");
        assertThat(last.getSnapRedirectUrl()).contains("vtweb/tok-1");
        assertThat(last.getExpiryAt()).isAfter(before.plusMinutes(29));

        // Outbox: aggregateId + eventType + topic + key + payload
        ArgumentCaptor<PaymentCreatedEvent> payload = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
        verify(outboxService).record(
                eq("ORD-1"), eq("payment.created.v1"), eq("payment.created.v1"), eq("ORD-1"), payload.capture());
        PaymentCreatedEvent created = payload.getValue();
        assertThat(created.orderId()).isEqualTo("ORD-1");
        assertThat(created.paymentId()).isEqualTo(99L);
        assertThat(created.snapToken()).isEqualTo("tok-1");
        assertThat(created.redirectUrl()).contains("vtweb/tok-1");
        assertThat(created.expiryAt()).isNotNull();
    }

    @Test
    void initiate_usesDefaultExpiry_whenEventExpiryNullOrZero() {
        when(paymentRepository.existsByOrderId("ORD-1")).thenReturn(false);
        stubSaveAssignsId();
        when(midtransClient.createSnapTransaction(any())).thenReturn(
                new SnapChargeResponse("tok-1", "url"));

        service.initiate(event("IDR", null));

        ArgumentCaptor<SnapChargeRequest> req = ArgumentCaptor.forClass(SnapChargeRequest.class);
        verify(midtransClient).createSnapTransaction(req.capture());
        assertThat(req.getValue().expiry().duration()).isEqualTo(60);   // DEFAULT_EXPIRY_MINUTES
    }

    @Test
    void initiate_defaultsCurrencyToIdr_whenEventCurrencyNull() {
        when(paymentRepository.existsByOrderId("ORD-1")).thenReturn(false);
        stubSaveAssignsId();
        when(midtransClient.createSnapTransaction(any())).thenReturn(
                new SnapChargeResponse("tok-1", "url"));

        service.initiate(event(null, 30));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("IDR");
    }

    // ---------------------------------------------------------------- idempotency

    @Test
    void initiate_isIdempotent_whenPaymentAlreadyExists() {
        when(paymentRepository.existsByOrderId("ORD-1")).thenReturn(true);

        service.initiate(event("IDR", 30));

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(midtransClient, outboxService);
    }

    // ---------------------------------------------------------------- midtrans failure

    @Test
    void initiate_propagatesMidtransException_andSkipsOutbox() {
        when(paymentRepository.existsByOrderId("ORD-1")).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(midtransClient.createSnapTransaction(any()))
                .thenThrow(new MidtransException("Snap ditolak (401)"));

        assertThatThrownBy(() -> service.initiate(event("IDR", 30)))
                .isInstanceOf(MidtransException.class);

        verifyNoInteractions(outboxService);
    }
}
