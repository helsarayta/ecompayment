package com.heydie.ecompayment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.kafka.topic")
public record KafkaTopicProperties(
        @DefaultValue("payment.request.v1")
        String paymentRequest,

        @DefaultValue("payment.created.v1")
        String paymentCreated,

        @DefaultValue("payment.result.v1")
        String paymentResult
) {}
