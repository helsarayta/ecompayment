package com.heydie.ecompayment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic paymentRequestTopic(KafkaTopicProperties t) {
        return TopicBuilder.name(t.paymentRequest())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentRequestDltTopic(KafkaTopicProperties t) {
        return TopicBuilder.name(t.paymentRequest() + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentCreateTopic(KafkaTopicProperties t) {
        return TopicBuilder.name(t.paymentCreated())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentResultTopic(KafkaTopicProperties t) {
        return TopicBuilder.name(t.paymentResult())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
