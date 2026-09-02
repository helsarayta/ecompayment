package com.heydie.ecompayment.payment.messaging.consumer;

import com.heydie.ecompayment.payment.messaging.dto.PaymentRequestedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private final PaymentRequestHandler paymentRequestHandler;

    public PaymentRequestListener(PaymentRequestHandler paymentRequestHandler) {
        this.paymentRequestHandler = paymentRequestHandler;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-request}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(@Payload PaymentRequestedEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          Acknowledgment ack) {
        paymentRequestHandler.handle(event, topic); // throw → tidak di-ack → DefaultErrorHandler (retry → DLT)
        ack.acknowledge(); // sukses → commit offset
    }
}
