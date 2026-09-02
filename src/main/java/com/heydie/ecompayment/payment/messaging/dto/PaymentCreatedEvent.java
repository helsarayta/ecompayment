package com.heydie.ecompayment.payment.messaging.dto;

import java.time.Instant;

public record PaymentCreatedEvent(
        String eventId,
        String orderId,
        Long paymentId,
        String snapToken,
        String redirectUrl,
        Instant expiryAt
) {
}
