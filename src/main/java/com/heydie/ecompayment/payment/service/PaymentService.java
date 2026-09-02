package com.heydie.ecompayment.payment.service;

import com.heydie.ecompayment.config.KafkaTopicProperties;
import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.entity.Payment;
import com.heydie.ecompayment.entity.enumeration.Status;
import com.heydie.ecompayment.midtrans.MidtransClient;
import com.heydie.ecompayment.midtrans.dto.SnapChargeRequest;
import com.heydie.ecompayment.midtrans.dto.SnapChargeResponse;
import com.heydie.ecompayment.outbox.OutboxService;
import com.heydie.ecompayment.payment.messaging.dto.PaymentCreatedEvent;
import com.heydie.ecompayment.payment.messaging.dto.PaymentRequestedEvent;
import com.heydie.ecompayment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int DEFAULT_EXPIRY_MINUTES = 60;
    private static final String EVENT_PAYMENT_CREATED = "payment.created.v1";

    private final PaymentRepository paymentRepository;
    private final MidtransClient midtransClient;
    private final MidtransProperties midtransProperties;
    private final OutboxService outboxService;
    private final KafkaTopicProperties kafkaTopicProperties;

    public PaymentService(PaymentRepository paymentRepository, MidtransClient midtransClient,
                          MidtransProperties midtransProperties, OutboxService outboxService,
                          KafkaTopicProperties kafkaTopicProperties) {
        this.paymentRepository = paymentRepository;
        this.midtransClient = midtransClient;
        this.midtransProperties = midtransProperties;
        this.outboxService = outboxService;
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Transactional
    public void initiate(PaymentRequestedEvent event) {
     if(paymentRepository.existsByOrderId(event.orderId())) {
         log.info("Payment untuk order {} sudah ada - skip (idempoten)", event.orderId());
         return;
     }

     int attempts = 1;
     String midtransOrderId = event.orderId() + "-" + attempts;
     int expiryMinutes = (event.expiryMinutes() != null && event.expiryMinutes() > 0)
             ? event.expiryMinutes() : DEFAULT_EXPIRY_MINUTES;
     LocalDateTime expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(expiryMinutes);

     Payment payment = Payment.builder()
             .orderId(event.orderId())
             .midtransOrderId(midtransOrderId)
             .grossAmount(event.amount())
             .currency(event.currency() == null || event.currency().isBlank() ? "IDR" : event.currency())
             .status(Status.INITIATED)
             .attempt(attempts)
             .expiryAt(expiryAt)
             .build();

     paymentRepository.save(payment);

        SnapChargeResponse snap = midtransClient.createSnapTransaction(
                SnapChargeRequest.of(midtransOrderId, event.amount().longValueExact(),
                        expiryMinutes, midtransProperties.finishRedirectUrl()));

        payment.setSnapToken(snap.token());
        payment.setSnapRedirectUrl(snap.redirectUrl());
        payment.setStatus(Status.PENDING);
        paymentRepository.save(payment);

        PaymentCreatedEvent created = new PaymentCreatedEvent(
                UUID.randomUUID().toString(), payment.getOrderId(), payment.getId(),
                snap.token(), snap.redirectUrl(), expiryAt.toInstant(ZoneOffset.UTC));

        outboxService.record(payment.getOrderId(), EVENT_PAYMENT_CREATED,
                kafkaTopicProperties.paymentCreated(), payment.getOrderId(), created);

        log.info("Snap dibuat: order={} payemtnId={}", event.orderId(), payment.getId());
    }


}
