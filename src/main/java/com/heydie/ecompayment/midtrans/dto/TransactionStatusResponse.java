package com.heydie.ecompayment.midtrans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionStatusResponse(
        String orderId,
        String transactionId,
        String transactionStatus,
        String fraudStatus,
        String statusCode,
        String statusMessage,
        String grossAmount,
        String paymentType,
        String transactionTime,
        String settlementTime,
        String signatureKey
) {
}
