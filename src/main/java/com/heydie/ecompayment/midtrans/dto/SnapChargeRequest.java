package com.heydie.ecompayment.midtrans.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapChargeRequest(
        @JsonProperty("transaction_details")
        TransactionDetails transactionDetails,

        Expiry expiry,

        @JsonProperty("credit_card")
        CreditCard creditCard,

        Callbacks callbacks
) {

    public record TransactionDetails(
            @JsonProperty("order_id")
            String orderId,

            @JsonProperty("gross_amount")
            long grossAmount
    ){}

    public record Expiry(
            String unit,

            int duration
    ){}

    public record CreditCard(
            boolean secure
    ){}

    public record Callbacks(
           String finish
    ){}

    public static SnapChargeRequest of(String midtransOrderId, long grossAmount,
            int expiryMinutes, String finishUrl) {
        return new SnapChargeRequest(
                new TransactionDetails(midtransOrderId, grossAmount),
                expiryMinutes > 0 ? new Expiry("minutes", expiryMinutes) : null,
                new CreditCard(true),
                (finishUrl == null || finishUrl.isBlank()) ? null : new Callbacks(finishUrl)
        );
    }
}
