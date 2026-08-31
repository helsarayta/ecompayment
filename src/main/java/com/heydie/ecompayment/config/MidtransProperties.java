package com.heydie.ecompayment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "midtrans")
public record MidtransProperties(

        @DefaultValue("false")
        boolean production,

        String serverKey,

        @DefaultValue("https://app.sandbox.midtrans.com")
        String snapBaseUrl,

        @DefaultValue("https://app.sandbox.midtrans.com")
        String apiBaseUrl,

        @DefaultValue("5s")
        Duration connectTimeOut,

        @DefaultValue("15s")
        Duration readTimeOut,

        String finishRedirectUrl

) {

    public String snapTransactionUrl() {
        return snapBaseUrl + "/snap/v1/transactions";
    }

    public String transactionStatusUrl(String midtransOrderId) {
        return apiBaseUrl + "/v2/" + midtransOrderId + "/status";
    }

}
