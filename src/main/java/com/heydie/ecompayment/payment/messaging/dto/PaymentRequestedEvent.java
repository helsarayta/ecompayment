package com.heydie.ecompayment.payment.messaging.dto;

import java.math.BigInteger;

public record PaymentRequestedEvent(
        String eventId,
        String orderId,
        BigInteger amount,
        String currency,
        Integer expiryMinutes
) {
}
