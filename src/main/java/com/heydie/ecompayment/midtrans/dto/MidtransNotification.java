package com.heydie.ecompayment.midtrans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MidtransNotification(
        String orderId,
        String statusCode,
        String grossAmount,
        String signatureKey,
        String transactionStatus,
        String fraudStatus,
        String transactionId,
        String paymentType,
        String transactionTime,
        String settlementTime,
        String statusMessage
) {
}
